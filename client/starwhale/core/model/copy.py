from __future__ import annotations

import os
import shutil
import struct
import typing as t
import hashlib
import logging
import functools
import contextvars
from pathlib import Path

import trio
import httpx
import lz4.block
from rich.progress import TaskID, Progress
from google.protobuf import json_format

from starwhale.base import cloud_blob_cache
from starwhale.utils import console, convert_to_bytes
from starwhale.consts import SW_API_VERSION, DEFAULT_MANIFEST_NAME
from starwhale.proto_gen import model_package_storage_pb2 as pb2
from starwhale.base.blob.store import LocalFileStore
from starwhale.base.uri.project import Instance
from starwhale.base.uri.resource import Resource

logging.getLogger("httpx").setLevel(logging.WARNING)

MAX_DATA_BLOB_SIZE = convert_to_bytes(
    os.environ.get("SW_BUNDLE_COPY_DATA_BLOB_SIZE", "8mi")
)
MAX_DATA_CHUNK_SIZE = convert_to_bytes("64ki")
MAX_RETRIES = int(os.environ.get("SW_BUNDLE_COPY_DATA_NET_RETRY", "10"))

_progress: contextvars.ContextVar[Progress] = contextvars.ContextVar("progress")
_task_id: contextvars.ContextVar[TaskID] = contextvars.ContextVar("task_id")
_file_sem: contextvars.ContextVar[trio.Semaphore] = contextvars.ContextVar("file_sem")
_net_sem: contextvars.ContextVar[trio.Semaphore] = contextvars.ContextVar("net_sem")
_blob_sem: contextvars.ContextVar[trio.Semaphore] = contextvars.ContextVar("blob_sem")
_cpu_limiter: contextvars.ContextVar[trio.CapacityLimiter] = contextvars.ContextVar(
    "compress_limiter"
)
_httpx_client: contextvars.ContextVar[httpx.AsyncClient] = contextvars.ContextVar(
    "httpx_client"
)
_instance: contextvars.ContextVar[Instance] = contextvars.ContextVar("instance_url")


class _Node:
    def __init__(self, path: Path):
        self.path = path
        self.file_proto = pb2.File()
        self.file_proto.type = pb2.FILE_TYPE_DIRECTORY
        self.dirs: t.List[_Node] = []
        self.files: t.List[pb2.File] = []
        self.links: t.List[pb2.File] = []


def _prepare_common_contextvars() -> None:
    cpu_count = os.cpu_count()
    if cpu_count is None:
        cpu_count = 32
    _cpu_limiter.set(trio.CapacityLimiter(cpu_count * 2))
    _file_sem.set(
        trio.Semaphore(int(os.environ.get("SW_BUNDLE_COPY_DISK_TASKS", "10")))
    )
    _net_sem.set(trio.Semaphore(int(os.environ.get("SW_BUNDLE_COPY_NET_TASKS", "16"))))
    _blob_sem.set(
        trio.Semaphore(int(os.environ.get("SW_BUNDLE_COPY_BUFFER_BLOBS", "32")))
    )
    timeout = httpx.Timeout(
        float(os.environ.get("SW_BUNDLE_COPY_DATA_NET_TIMEOUT", "90"))
    )
    limits = httpx.Limits(
        max_keepalive_connections=_net_sem.get().value,
        max_connections=_net_sem.get().value,
    )
    _httpx_client.set(httpx.AsyncClient(timeout=timeout, limits=limits))


async def _send_request(
    method: str,
    url: str,
    retry_count: int,
    params: t.Dict[str, t.Any] | None = None,
    content: str | bytes | None = None,
    json: t.Any | None = None,
    headers: t.Dict[str, str] | None = None,
) -> httpx.Response | None:
    headers = headers or {}
    resp = await _httpx_client.get().request(
        method,
        url,
        params=params,
        content=content,
        json=json,
        headers=headers,
        follow_redirects=True,
    )
    if resp.is_success:
        return resp
    if (
        resp.status_code not in (408, 429, 500, 502, 503, 504)
        or retry_count == MAX_RETRIES
    ):
        data = await resp.aread()
        await resp.aclose()
        raise httpx.HTTPStatusError(
            f"'{resp.status_code} {resp.reason_phrase}' for url '{resp.url}', "
            + f"message={data.decode('utf-8')}",
            request=resp.request,
            response=resp,
        )
    return None


async def _http_request(
    method: str,
    url: str | None = None,
    path: str | None = None,
    params: t.Dict[str, t.Any] | None = None,
    content: str | bytes | None = None,
    json: t.Any | None = None,
    headers: t.Dict[str, str] | None = None,
    replace: bool = True,
) -> httpx.Response:
    headers = headers or {}
    instance = _instance.get()
    headers = dict(headers)
    if url is None:
        assert path is not None
        url = f"{instance.url}/api/{SW_API_VERSION}/{path.lstrip('/')}"
        headers["Authorization"] = instance.token

    url_iter = cloud_blob_cache.replace_url(url, replace)
    try:
        interval = 0.0
        retry_count = 0
        while True:
            retry_count += 1
            try:
                resp = await _send_request(
                    method, next(url_iter), retry_count, params, content, json, headers
                )
                if resp is not None:
                    return resp
            except httpx.RequestError as e:
                if retry_count == MAX_RETRIES:
                    raise e
            await trio.sleep(interval)
            if interval == 0.0:
                interval = 0.5
            else:
                interval *= 2
                if interval > 10:
                    interval = 10
    finally:
        url_iter.close()


async def _upload_blob(b: bytes) -> str:
    assert len(b) > 0
    async with _net_sem.get():
        res = await _http_request(
            "POST",
            path="/blob",
            json={
                "contentMd5": hashlib.md5(b).hexdigest(),
                "contentLength": len(b),
            },
        )
        result = res.json()["data"]
        blob_id = result["blobId"]
        if result["status"] == "OK":
            await _http_request(
                "PUT",
                url=result["signedUrl"],
                content=b,
                headers={"Content-Type": "application/octet-stream"},
                replace=False,
            )
            res = await _http_request("POST", path=f"/blob/{blob_id}", json={})
            blob_id = res.json()["data"]["blobId"]
        return t.cast(str, blob_id)


async def _compress_and_upload_blob(
    compression: int | None,
    chunk_channel: trio.MemoryReceiveChannel[bytes],
    compression_channel: trio.MemorySendChannel[int],
    blob_id_setter: t.Callable[[str], None],
) -> None:
    async with compression_channel:
        blob_size = 0
        b = bytearray()
        async with chunk_channel:
            async for chunk in chunk_channel:
                assert len(chunk) > 0
                assert len(chunk) <= MAX_DATA_CHUNK_SIZE
                blob_size += len(chunk)
                if compression == pb2.COMPRESSION_ALGORITHM_NO_COMPRESSION:
                    b.extend(chunk)
                else:
                    compressed = await trio.to_thread.run_sync(
                        functools.partial(lz4.block.compress, chunk, store_size=False),
                        limiter=_cpu_limiter.get(),
                    )
                    if compression is None:
                        if len(compressed) < len(chunk) * 0.9:
                            compression = pb2.COMPRESSION_ALGORITHM_LZ4
                        else:
                            compression = pb2.COMPRESSION_ALGORITHM_NO_COMPRESSION
                        await compression_channel.send(compression)
                        if compression == pb2.COMPRESSION_ALGORITHM_NO_COMPRESSION:
                            b.extend(chunk)
                            continue
                    if len(compressed) >= 65536:
                        mid = len(chunk) // 2
                        c1 = await trio.to_thread.run_sync(
                            functools.partial(
                                lz4.block.compress, chunk[:mid], store_size=False
                            ),
                            limiter=_cpu_limiter.get(),
                        )
                        c2 = await trio.to_thread.run_sync(
                            functools.partial(
                                lz4.block.compress, chunk[mid:], store_size=False
                            ),
                            limiter=_cpu_limiter.get(),
                        )
                        b.extend(struct.pack(">H", len(c1)))
                        b.extend(c1)
                        b.extend(struct.pack(">H", len(c2)))
                        b.extend(c2)
                    else:
                        b.extend(struct.pack(">H", len(compressed)))
                        b.extend(compressed)
        blob_id_setter(await _upload_blob(bytes(b)))
        _progress.get().advance(_task_id.get(), blob_size)
        _blob_sem.get().release()


async def _upload_file(path: Path, file: pb2.File) -> None:
    md5 = hashlib.md5()
    compression_send, compression_recv = trio.open_memory_channel[t.Any](1)
    async with await trio.open_file(path, "rb") as f:
        async with trio.open_nursery() as nursery:
            compression = None
            while True:
                await _blob_sem.get().acquire()
                chunk_send, chunk_recv = trio.open_memory_channel[bytes](0)
                blob_index = len(file.blob_ids)
                file.blob_ids.append("")

                def set_blob_id(index: int, blob_id: str) -> None:
                    file.blob_ids[index] = blob_id

                nursery.start_soon(
                    _compress_and_upload_blob,
                    compression,
                    chunk_recv,
                    compression_send,
                    functools.partial(set_blob_id, blob_index),
                )
                blob_size = 0
                async with chunk_send:
                    while blob_size < MAX_DATA_BLOB_SIZE:
                        chunk = b""
                        while len(chunk) < MAX_DATA_CHUNK_SIZE:
                            b: bytes = await f.read(MAX_DATA_CHUNK_SIZE)
                            if b == b"":
                                break
                            chunk += b
                        if chunk == b"":
                            break
                        await chunk_send.send(chunk)
                        blob_size += len(chunk)
                        md5.update(chunk)
                if compression is None:
                    compression = await compression_recv.receive()
                    file.compression_algorithm = compression
                if blob_size < MAX_DATA_BLOB_SIZE:
                    break
            file.md5 = md5.digest()
    _file_sem.get().release()


async def _merge_files(
    meta_blob: pb2.MetaBlob, ch: trio.MemoryReceiveChannel[t.Tuple[Path, pb2.File]]
) -> None:
    buf = b""
    files: t.List[pb2.File] = []
    blob_size = 0
    async with ch:
        async for path, file in ch:
            async with await trio.open_file(path, "rb") as f:
                b = await f.read()
                file.md5 = hashlib.md5(b).digest()
                compressed = await trio.to_thread.run_sync(
                    functools.partial(lz4.block.compress, b, store_size=False),
                    limiter=_cpu_limiter.get(),
                )
                if len(compressed) + 2 < len(b):
                    file.compression_algorithm = pb2.COMPRESSION_ALGORITHM_LZ4
                    compressed = struct.pack(">H", len(compressed)) + compressed
                else:
                    file.compression_algorithm = (
                        pb2.COMPRESSION_ALGORITHM_NO_COMPRESSION
                    )
                    compressed = b
            if len(buf) + len(compressed) > MAX_DATA_BLOB_SIZE:
                blob_id = await _upload_blob(buf)
                _progress.get().advance(_task_id.get(), blob_size)
                for i in files:
                    i.blob_ids.append(blob_id)
                buf = b""
                files = []
                blob_size = 0
            file.blob_offset = len(buf)
            file.blob_size = len(compressed)
            files.append(file)
            buf += compressed
            blob_size += len(b)
    if len(buf) < 4096:
        meta_blob.data = buf
        blob_id = ""
    else:
        blob_id = await _upload_blob(buf)
    for i in files:
        i.blob_ids.append(blob_id)
    _progress.get().advance(_task_id.get(), blob_size)


def _scan_dir(workdir: Path) -> t.List[_Node]:
    nodes = [_Node(workdir)]
    inode_path_dict: t.Dict[int, Path] = {}
    i = 0
    while i < len(nodes):
        for child in sorted(nodes[i].path.glob("*")):
            if i == 0 and child.name != "src" and child.name != DEFAULT_MANIFEST_NAME:
                continue
            stat = child.stat()
            if child.is_dir() and not child.is_symlink():
                new_node = _Node(child)
                new_node.file_proto.name = child.name
                new_node.file_proto.type = pb2.FILE_TYPE_DIRECTORY
                new_node.file_proto.permission = stat.st_mode
                nodes[i].dirs.append(new_node)
                nodes.append(new_node)
            else:
                file = pb2.File()
                file.name = child.name
                if child.is_symlink():
                    file.type = pb2.FILE_TYPE_SYMLINK
                    file.link = os.readlink(child)
                    nodes[i].links.append(file)
                elif child.is_file():
                    if stat.st_ino in inode_path_dict:
                        file.type = pb2.FILE_TYPE_HARDLINK
                        file.link = (
                            inode_path_dict[stat.st_ino].relative_to(workdir).as_posix()
                        )
                        nodes[i].links.append(file)
                    else:
                        file.type = pb2.FILE_TYPE_REGULAR
                        file.size = stat.st_size
                        file.permission = stat.st_mode
                        nodes[i].files.append(file)
                        inode_path_dict[stat.st_ino] = child
        i += 1
    return nodes


async def _upload_runtime(
    func: t.Callable[[Progress], str | None], ch: trio.MemorySendChannel[str | None]
) -> None:
    async with ch:
        runtime_verison = await trio.to_thread.run_sync(func, _progress.get())
        await ch.send(runtime_verison)


def _prepare_meta_blobs(nodes: t.List[_Node], meta_blobs: t.List[pb2.MetaBlob]) -> None:
    files: t.List[pb2.File] = [nodes[0].file_proto]
    for node in nodes:
        node.file_proto.from_file_index = len(files)
        files.extend([dir.file_proto for dir in node.dirs])
        files.extend(node.files)
        for f in node.files:
            if f.type == pb2.FILE_TYPE_HUGE:
                f.from_file_index = len(files)
                for i in range(0, len(f.blob_ids), 100):
                    ref_file = pb2.File()
                    ref_file.blob_ids.extend(f.blob_ids[i : i + 100])
                    files.append(ref_file)
                del f.blob_ids[:]
                f.to_file_index = len(files)
        files.extend(node.links)
        node.file_proto.to_file_index = len(files)
    estimated_blob_size = meta_blobs[0].ByteSize()
    for file in files:
        size = file.ByteSize()
        if size < 128:
            size += 1
        elif size < 16384:
            size += 2
        else:
            size += 3
        size += 1
        if estimated_blob_size + size > 65536:
            meta_blobs.append(pb2.MetaBlob())
            estimated_blob_size = 0
        meta_blobs[-1].files.append(file)
        estimated_blob_size += size


async def _upload_meta_blobs(meta_blobs: t.List[pb2.MetaBlob]) -> str:
    async with trio.open_nursery() as nursery:
        first_file_index = 0
        for blob in meta_blobs:
            if first_file_index > 0:
                index = meta_blobs[0].meta_blob_indexes.add()

                async def update_index(
                    index: pb2.MetaBlobIndex, blob: pb2.MetaBlob
                ) -> None:
                    index.blob_id = await _upload_blob(blob.SerializeToString())

                nursery.start_soon(update_index, index, blob)
                index.last_file_index = first_file_index + len(blob.files) - 1
            first_file_index += len(blob.files)
    blob_id = await _upload_blob(meta_blobs[0].SerializeToString())
    return blob_id


async def _upload_files(nodes: t.List[_Node], meta_blob: pb2.MetaBlob) -> None:
    files_send, files_recv = trio.open_memory_channel[t.Tuple[Path, pb2.File]](0)
    async with trio.open_nursery() as nursery:
        nursery.start_soon(_merge_files, meta_blob, files_recv)
        for node in nodes:
            for file in node.files:
                if file.size < 4096:
                    await files_send.send((node.path / file.name, file))
                else:
                    await _file_sem.get().acquire()
                    nursery.start_soon(
                        _upload_file,
                        node.path / file.name,
                        file,
                    )
        await files_send.aclose()


async def upload_model(
    dest_uri: Resource,
    workdir: Path,
    progress: Progress,
    upload_runtime: t.Callable[[Progress], str | None],
    force: bool,
) -> None:
    cloud_blob_cache.init()
    _progress.set(progress)
    _instance.set(dest_uri.instance)
    _prepare_common_contextvars()
    async with _httpx_client.get():
        async with trio.open_nursery() as nursery:
            runtime_version_send, runtime_version_recv = trio.open_memory_channel[
                t.Optional[str]
            ](1)
            nursery.start_soon(_upload_runtime, upload_runtime, runtime_version_send)
            console.print(f"scanning {workdir.name}...")
            nodes = _scan_dir(workdir)
            console.print("scan done")
            _task_id.set(
                progress.add_task(
                    f":arrow_up: uploading {workdir.name}...",
                    total=sum([file.size for node in nodes for file in node.files]),
                )
            )
            meta_blobs = [pb2.MetaBlob()]
            await _upload_files(nodes, meta_blobs[0])
            _prepare_meta_blobs(nodes, meta_blobs)
            console.print("uploading metadata...")
            blob_id = await _upload_meta_blobs(meta_blobs)
            runtime_version = await runtime_version_recv.receive()

        _request_json: t.Dict = {
            "metaBlobId": blob_id,
            "builtInRuntime": runtime_version,
            "force": force,
        }
        _finetune_id = os.environ.get("SW_SERVER_TRIGGERED_FINETUNE_ID")
        if _finetune_id is not None:
            _request_json["modelSource"] = {
                "type": "FINE_TUNE",
                "id": int(_finetune_id),
            }
        await _http_request(
            "POST",
            path=(
                f"/project/{dest_uri.project.id}"
                f"/model/{dest_uri.name}"
                f"/version/{dest_uri.version}/completeUpload"
            ),
            json=_request_json,
            replace=False,
        )
        console.print("metadata uploaded")


async def _download_meta_blobs(src_uri: Resource) -> t.List[pb2.MetaBlob]:
    meta_path = (
        f"/project/{src_uri.project.id}"
        + f"/model/{src_uri.name}"
        + f"/version/{src_uri.version}/meta"
    )
    res = await _http_request(
        "GET",
        path=meta_path,
    )
    meta_blobs = [pb2.MetaBlob()]
    json_format.Parse(res.json()["data"], meta_blobs[0])
    async with trio.open_nursery() as nursery:
        for index in meta_blobs[0].meta_blob_indexes:

            async def download(meta_blob: pb2.MetaBlob, blob_id: str) -> None:
                res = await _http_request(
                    "GET", path=meta_path, params={"blobId": blob_id}
                )
                json_format.Parse(res.json()["data"], meta_blob)

            meta_blobs.append(pb2.MetaBlob())
            nursery.start_soon(download, meta_blobs[-1], index.blob_id)
    return meta_blobs


def _prepare_nodes(meta_blobs: t.List[pb2.MetaBlob], workdir: Path) -> t.List[_Node]:
    def meta_json() -> str:
        ret = ""
        for meta_blob in meta_blobs:
            ret += "\n" + json_format.MessageToJson(meta_blob)
        return ret

    files: t.List[pb2.File] = []
    for meta_blob in meta_blobs:
        files.extend(meta_blob.files)
    nodes = [_Node(workdir)]
    nodes[0].file_proto = files[0]
    used = set()
    used.add(0)
    i = 0
    while i < len(nodes):
        for j in range(
            nodes[i].file_proto.from_file_index, nodes[i].file_proto.to_file_index
        ):
            if j in used:
                raise RuntimeError(
                    f"invalid meta blob. file {j} is used multiple times." + meta_json()
                )
            used.add(j)
            if files[j].type == pb2.FILE_TYPE_DIRECTORY:
                nodes.append(_Node(nodes[i].path / files[j].name))
                nodes[-1].file_proto = files[j]
                nodes[i].dirs.append(nodes[-1])
            elif (
                files[j].type == pb2.FILE_TYPE_SYMLINK
                or files[j].type == pb2.FILE_TYPE_HARDLINK
            ):
                nodes[i].links.append(files[j])
            else:
                if files[j].type == pb2.FILE_TYPE_HUGE:
                    for k in range(files[j].from_file_index, files[j].to_file_index):
                        files[j].blob_ids.extend(files[k].blob_ids)
                        files[j].signed_urls.extend(files[k].signed_urls)
                        if k in used:
                            raise RuntimeError(
                                f"invalid meta blob. file {k} is used multiple times."
                                + meta_json()
                            )
                        used.add(k)
                nodes[i].files.append(files[j])
        i += 1
    for i in range(len(files)):
        if i not in used:
            raise RuntimeError(
                f"invalid meta blob. file {i} is never used." + meta_json()
            )
    return nodes


async def _download_blob(
    compression: int,
    url: str,
    index: int,
    ch: trio.MemorySendChannel[t.Tuple[int, bytes]],
) -> None:
    async with _net_sem.get():
        data = (await _http_request("GET", url=url)).content
    if compression == pb2.COMPRESSION_ALGORITHM_NO_COMPRESSION:
        blob = data
    else:
        buf = bytearray()
        i = 0
        while i < len(data):
            assert i + 2 <= len(data)
            size = struct.unpack(">H", data[i : i + 2])[0]
            i += 2
            assert i + size <= len(data)
            buf.extend(
                await trio.to_thread.run_sync(
                    functools.partial(
                        lz4.block.decompress,
                        data[i : i + size],
                        uncompressed_size=MAX_DATA_CHUNK_SIZE,
                    ),
                    limiter=_cpu_limiter.get(),
                )
            )
            i += size
        blob = bytes(buf)
    await ch.send((index, blob))


async def _write_blob(
    store: LocalFileStore,
    path: Path,
    file: pb2.File,
    total: int,
    ch: trio.MemoryReceiveChannel[t.Tuple[int, bytes]],
) -> None:
    buf: t.Dict[int, bytes] = {}
    next = 0
    md5 = hashlib.md5()
    async with await trio.open_file(path, "wb") as f:
        async with ch:
            for _ in range(total):
                index, b = await ch.receive()
                if index == next:
                    await f.write(b)
                    md5.update(b)
                    _blob_sem.get().release()
                    _progress.get().advance(_task_id.get(), len(b))
                    next += 1
                    while next in buf:
                        md5.update(buf[next])
                        await f.write(buf[next])
                        del buf[next]
                        next += 1
                        _blob_sem.get().release()
                        _progress.get().advance(_task_id.get(), len(b))
                else:
                    assert index not in buf
                    assert index > next
                    buf[index] = b
    if file.md5 != md5.digest():
        raise RuntimeError(
            f"downloaded file {path} md5 mismatch. expected: {file.md5.hex()}, "
            + f"actual: {md5.hexdigest()}"
        )
    await trio.to_thread.run_sync(store.link, path, file.md5.hex())
    await trio.to_thread.run_sync(os.chmod, path, file.permission)


def get_file_from_cache(store: LocalFileStore, path: Path, file: pb2.File) -> bool:
    f = store.get(file.md5.hex())
    if f is not None and f.exists():
        if path.exists():
            path.unlink()
        f.link(path)
        os.chmod(path, file.permission)
        _progress.get().advance(_task_id.get(), file.size)
        return True
    return False


async def _download_file(path: Path, file: pb2.File, store: LocalFileStore) -> None:
    if not await trio.to_thread.run_sync(get_file_from_cache, store, path, file):
        blob_send, blob_recv = trio.open_memory_channel[t.Tuple[int, bytes]](0)
        async with trio.open_nursery() as nursery:
            nursery.start_soon(
                _write_blob, store, path, file, len(file.signed_urls), blob_recv
            )
            for i, url in enumerate(file.signed_urls):
                await _blob_sem.get().acquire()
                nursery.start_soon(
                    _download_blob, file.compression_algorithm, url, i, blob_send
                )
    _file_sem.get().release()


async def _download_small_files(
    blob0: bytes,
    ch: trio.MemoryReceiveChannel[t.Tuple[Path, pb2.File]],
    store: LocalFileStore,
) -> None:
    async def save_file(path: Path, file: pb2.File, b: bytes) -> None:
        if file.compression_algorithm == pb2.COMPRESSION_ALGORITHM_LZ4:
            size = struct.unpack(">H", b[:2])[0]
            assert size == len(b) - 2
            data = await trio.to_thread.run_sync(
                functools.partial(
                    lz4.block.decompress,
                    b[2:],
                    uncompressed_size=MAX_DATA_CHUNK_SIZE,
                ),
                limiter=_cpu_limiter.get(),
            )
        else:
            data = b
        assert len(data) == file.size
        async with _file_sem.get():
            async with await trio.open_file(path, "wb") as f:
                await f.write(data)
            await trio.to_thread.run_sync(store.link, path, file.md5.hex())
            await trio.to_thread.run_sync(os.chmod, path, file.permission)
        _progress.get().advance(_task_id.get(), file.size)

    blob_id = "invalid"
    buf = b""
    async with trio.open_nursery() as nursery:
        async with ch:
            async for path, file in ch:
                async with _file_sem.get():
                    if await trio.to_thread.run_sync(
                        get_file_from_cache, store, path, file
                    ):
                        continue
                if file.blob_ids[0] == "":
                    nursery.start_soon(
                        save_file,
                        path,
                        file,
                        blob0[file.blob_offset : file.blob_offset + file.blob_size],
                    )
                else:
                    if file.blob_ids[0] != blob_id:
                        async with _net_sem.get():
                            buf = (
                                await _http_request("GET", file.signed_urls[0])
                            ).content
                        blob_id = file.blob_ids[0]
                    nursery.start_soon(
                        save_file,
                        path,
                        file,
                        buf[file.blob_offset : file.blob_offset + file.blob_size],
                    )


async def download_model(
    src_uri: Resource,
    workdir: Path,
    store: LocalFileStore,
    progress: Progress,
    force: bool,
) -> None:
    cloud_blob_cache.init()
    _progress.set(progress)
    _instance.set(src_uri.instance)
    _prepare_common_contextvars()
    async with _httpx_client.get():
        console.print(":arrow_down: downloading metadata...")
        meta_blobs = await _download_meta_blobs(src_uri)
        console.print("metadata downloaded")
        nodes = _prepare_nodes(meta_blobs, workdir)
        console.print("creating directories...")
        for node in nodes[1:]:
            os.makedirs(node.path, node.file_proto.permission, exist_ok=True)
        console.print("directories created")
        _task_id.set(
            progress.add_task(
                ":arrow_down: downloading files...",
                total=sum([file.size for node in nodes for file in node.files]),
            )
        )
        files_send, files_recv = trio.open_memory_channel[t.Tuple[Path, pb2.File]](0)
        async with trio.open_nursery() as nursery:
            nursery.start_soon(
                _download_small_files, meta_blobs[0].data, files_recv, store
            )
            async with files_send:
                for node in nodes:
                    for file in node.files:
                        path = node.path / file.name
                        if file.size < 4096:
                            await files_send.send((path, file))
                        else:
                            await _file_sem.get().acquire()
                            nursery.start_soon(_download_file, path, file, store)
        for node in nodes:
            for link in node.links:
                dest = node.path / link.name
                if dest.is_symlink():
                    if force:
                        dest.unlink()
                    else:
                        raise RuntimeError(f"can not overwrite {dest}")
                elif dest.exists():
                    if force:
                        if dest.is_dir():
                            shutil.rmtree(dest)
                        else:
                            os.remove(dest)
                    else:
                        raise RuntimeError(f"can not overwrite {dest}")
                if link.type == pb2.FILE_TYPE_SYMLINK:
                    os.symlink(link.link, node.path / link.name)
                elif link.type == pb2.FILE_TYPE_HARDLINK:
                    os.link(workdir / link.link, node.path / link.name)
