from __future__ import annotations

import typing as t
from pathlib import Path

import click
from click_option_group import optgroup, MutuallyExclusiveOptionGroup

from starwhale.utils import random_str
from starwhale.consts import DefaultYAMLName, DEFAULT_PAGE_IDX, DEFAULT_PAGE_SIZE
from starwhale.base.type import DatasetChangeMode, DatasetFolderSourceType
from starwhale.utils.cli import AliasedGroup
from starwhale.utils.load import import_object
from starwhale.utils.error import NotFoundError
from starwhale.base.uri.resource import Resource, ResourceType
from starwhale.core.dataset.model import DatasetAttr, DatasetConfig

from .view import get_term_view, DatasetTermView


@click.group(
    "dataset",
    cls=AliasedGroup,
    help="Dataset management, build/info/list/copy/tag...",
)
@click.pass_context
def dataset_cmd(ctx: click.Context) -> None:
    ctx.obj = get_term_view(ctx.obj)


@dataset_cmd.command("build")
@optgroup.group(
    "\n  ** Acceptable build sources",
    cls=MutuallyExclusiveOptionGroup,
    help="The selector of the dataset build source, default is dataset.yaml source",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "image_folder",
    "-if",
    "--image",
    "--image-folder",
    help="Build dataset from image folder, the folder should contain the image files.",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "audio_folder",
    "-af",
    "--audio",
    "--audio-folder",
    help="Build dataset from audio folder, the folder should contain the audio files.",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "video_folder",
    "-vf",
    "--video",
    "--video-folder",
    help="Build dataset from video folder, the folder should contain the video files.",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "python_handler",
    "-h",
    "--handler",
    "--python-handler",
    help="Build dataset from python executor handler, the handler format is [module path]:[class or function name].",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "dataset_yaml",
    "-f",
    "--yaml",
    "--dataset-yaml",
    default=DefaultYAMLName.DATASET,
    help="Build dataset from dataset.yaml file. Default uses dataset.yaml in the work directory(pwd).",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "json_files",
    "-jf",
    "--json",
    multiple=True,
    help=(
        "Build dataset from json or json line files, local path or http downloaded url is supported."
        "For the json file: the json content structure should be a list[dict] or tuple[dict] from the original format or ingest format by field-selector."
        "For the json line file: each line is a json dict."
    ),
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "hf_repo",
    "-hf",
    "--huggingface",
    help=(
        "Build dataset from huggingface dataset, the huggingface option is a huggingface repo name"
    ),
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "csv_files",
    "-c",
    "--csv",
    multiple=True,
    help="Build dataset from csv files. The option is a csv file path, dir path or a http downloaded url."
    "The option can be used multiple times.",
)
@optgroup.group(
    "\n  ** Build Mode Selectors",
    cls=MutuallyExclusiveOptionGroup,
    help="The selector of build mode. If no set, the default is `patch` mode.",
)
@optgroup.option(  # type: ignore
    "-pt",
    "--patch",
    "mode",
    flag_value=DatasetChangeMode.PATCH.value,
    default=True,
    help="Patch mode, only update the changed rows and columns for the existed dataset.",
)
@optgroup.option(  # type: ignore
    "-ow",
    "--overwrite",
    "mode",
    flag_value=DatasetChangeMode.OVERWRITE.value,
    help="Overwrite mode, update records and delete extraneous rows from the existed dataset.",
)
@optgroup.group("\n  ** Global Configurations")
@optgroup.option("-n", "--name", help="Dataset name")  # type: ignore[no-untyped-call]
@optgroup.option(  # type: ignore[no-untyped-call]
    "-p",
    "--project",
    default="",
    help="Project URI, the default is the current selected project. The dataset will store in the specified project",
)
@optgroup.option("-d", "--desc", help="Dataset description")  # type: ignore[no-untyped-call]
@optgroup.option(  # type: ignore[no-untyped-call]
    "-as",
    "--alignment-size",
    help="swds-bin format dataset: alignment size",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "-vs",
    "--volume-size",
    help="swds-bin format dataset: volume size",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "tags",
    "-t",
    "--tag",
    multiple=True,
    help="dataset tags, the option can be used multiple times. `latest` and `^v\d+$` tags are reserved tags.",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "file_encoding",
    "--encoding",
    help="The csv/json/jsonl file encoding.",
)
@optgroup.option("-r", "--runtime", help="runtime uri")  # type: ignore[no-untyped-call]
@optgroup.group("\n  ** Handler Build Source Configurations")
@optgroup.option("-w", "--workdir", default=".", help="work dir to search handler, the option only works for the handler build source.")  # type: ignore[no-untyped-call]
@optgroup.group("\n  ** Folder(Video/Image/Audio) Build Configurations")
@optgroup.option(  # type: ignore[no-untyped-call]
    "--auto-label/--no-auto-label",
    is_flag=True,
    show_default=True,
    default=True,
    help="Whether to auto label by the sub-folder name. The default value is True",
)
@optgroup.group("\n  ** Json Build Source Configurations")
@optgroup.option(  # type: ignore[no-untyped-call]
    "json_field_selector",
    "--field-selector",
    default="",
    help=(
        "The filed from which you would like to extract dataset array items. The filed is split by the dot(.) symbol."
        "The default value is empty str, which indicates that the dict is an array contains all the items."
    ),
)
@optgroup.group("\n  ** Huggingface Build Source Configurations")
@optgroup.option(  # type: ignore[no-untyped-call]
    "hf_subsets",
    "--subset",
    multiple=True,
    help=(
        "Huggingface dataset subset name. If the subset name is not specified, the all subsets will be built."
        "The option can be used multiple times."
    ),
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "hf_split",
    "--split",
    help=(
        "Huggingface dataset split name. If the split name is not specified, the all splits dataset will be built."
    ),
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "hf_revision",
    "--revision",
    default="main",
    show_default=True,
    help=(
        "Version of the dataset script to load. Defaults to 'main'. The option value accepts tag name, or branch name, or commit hash."
    ),
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "hf_info",
    "--add-hf-info/--no-add-hf-info",
    is_flag=True,
    default=True,
    show_default=True,
    help="Whether to add huggingface dataset info to the dataset rows, currently support to add subset and split into the dataset rows."
    "subset uses _hf_subset field name, split uses _hf_split field name.",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "hf_cache",
    "--cache/--no-cache",
    is_flag=True,
    default=True,
    show_default=True,
    help="Whether to use huggingface dataset cache(download + local hf dataset).",
)
@optgroup.group("\n  ** CSV Files Build Source Configurations")
@optgroup.option(  # type: ignore[no-untyped-call]
    "csv_dialect",
    "--dialect",
    type=click.Choice(["excel", "excel-tab", "unix"]),
    default="excel",
    show_default=True,
    help="The csv file dialect, the default is excel.",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "csv_delimiter",
    "--delimiter",
    default=",",
    show_default=True,
    help="A one-character string used to separate fields for the csv file.",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "csv_quotechar",
    "--quotechar",
    default='"',
    show_default=True,
    help="A one-character string used to quote fields containing special characters,"
    "such as the delimiter or quotechar, or which contain new-line characters.",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "csv_skipinitialspace",
    "--skipinitialspace/--no-skipinitialspace",
    is_flag=True,
    default=False,
    show_default=True,
    help="Whether to skip spaces after delimiter for the csv file.",
)
@optgroup.option(  # type: ignore[no-untyped-call]
    "csv_strict",
    "--strict/--no-strict",
    is_flag=True,
    default=False,
    show_default=True,
    help="When True, raise exception Error if the csv is not well formed.",
)
@click.pass_obj
def _build(
    view: DatasetTermView,
    workdir: str,
    python_handler: str,
    name: str,
    project: str,
    desc: str,
    dataset_yaml: str,
    alignment_size: str,
    volume_size: str,
    runtime: str,
    image_folder: str,
    audio_folder: str,
    video_folder: str,
    auto_label: bool,
    json_files: t.List[str],
    json_field_selector: str,
    mode: str,
    hf_repo: str,
    hf_subsets: t.List[str],
    hf_split: str,
    hf_revision: str,
    hf_cache: bool,
    hf_info: bool,
    tags: t.List[str],
    csv_files: t.List[str],
    csv_dialect: str,
    csv_delimiter: str,
    csv_quotechar: str,
    csv_skipinitialspace: bool,
    csv_strict: bool,
    file_encoding: str,
) -> None:
    """Build Starwhale Dataset.
    This command only supports to build standalone dataset.

    Acceptable build sources:

        \b
        - dataset.yaml file: The default build source. The dataset.yaml includes the dataset build config and build script entrypoint.
            You should write some python code to build the dataset.
        - handler: The handler is a python executor, it should be a python class or function. When use the handler build source, you should specify --workdir option.
            if the workdir option is emitted, swcli will use the work directory(pwd) as the workdir.
        - image folder: The image-folder is a starwhale dataset builder designed to quickly build an image dataset with a folder of images without any code.
        - audio folder: The audio-folder is a starwhale dataset builder designed to quickly build an audio dataset with a folder of audios without any code.
        - video folder: The video-folder is a starwhale dataset builder designed to quickly build a video dataset with a folder of videos without any code.
        - json file: The json-file is a starwhale dataset builder designed to quickly build a dataset with a json file without any code.
        - huggingface dataset: The huggingface dataset is a starwhale dataset builder designed to quickly build a dataset from huggingface dataset without any code.
        - csv files: The csv-file is a starwhale dataset builder designed to quickly build a dataset with csv files without any code.

    Examples:

        \b
        - from dataset.yaml
        swcli dataset build  # build dataset from dataset.yaml in the current work directory(pwd)
        swcli dataset build --yaml /path/to/dataset.yaml  # build dataset from /path/to/dataset.yaml, all the involved files are related to the dataset.yaml file.
        swcli dataset build --overwrite --yaml /path/to/dataset.yaml  # build dataset from /path/to/dataset.yaml, and overwrite the existed dataset.
        swcli dataset build --tag tag1 --tag tag2

        \b
        - from handler
        swcli dataset build --handler mnist.dataset:iter_mnist_item # build dataset from mnist.dataset:iter_mnist_item handler, the workdir is the current work directory(pwd).
        # build dataset from mnist.dataset:LinkRawDatasetProcessExecutor handler, the workdir is example/mnist
        swcli dataset build --handler mnist.dataset:LinkRawDatasetProcessExecutor --workdir example/mnist

        \b
        - from image folder
        swcli dataset build --image-folder /path/to/image/folder  # build dataset from /path/to/image/folder, search all image type files.

        \b
        - from audio folder
        swcli dataset build --audio-folder /path/to/audio/folder  # build dataset from /path/to/audio/folder, search all audio type files.

        \b
        - from video folder
        swcli dataset build --video-folder /path/to/video/folder  # build dataset from /path/to/video/folder, search all video type files.

        \b
        - from json or json line files
        swcli dataset build --json /path/to/example.json
        swcli dataset build --json http://example.com/example.json
        swcli dataset build --json /path/to/example.json --field-selector a.b.c # extract the json_content["a"]["b"]["c"] field from the json file.
        swcli dataset build --name qald9 --json https://raw.githubusercontent.com/ag-sc/QALD/master/9/data/qald-9-test-multilingual.json --field-selector questions
        swcli dataset build --json /path/to/test01.jsonl --json /path/to/test02.jsonl
        swcli dataset build --json https://modelscope.cn/api/v1/datasets/damo/100PoisonMpts/repo\?Revision\=master\&FilePath\=train.jsonl

        \b
        - from huggingface dataset
        swcli dataset build --huggingface mnist
        swcli dataset build -hf mnist --no-cache
        swcli dataset build -hf cais/mmlu --subset anatomy --split auxiliary_train --revision 7456cfb

        \b
        - from csv files
        swcli dataset build --csv /path/to/example.csv
        swcli dataset build --csv /path/to/example.csv --csv-file /path/to/example2.csv
        swcli dataset build --csv /path/to/csv-dir
        swcli dataset build --csv http://example.com/example.csv
        swcli dataset build --name product-desc-modelscope --csv https://modelscope.cn/api/v1/datasets/lcl193798/product_description_generation/repo\?Revision\=master\&FilePath\=test.csv --encoding=utf-8-sig
    """
    # TODO: add dry-run
    # TODO: add compress args
    mode_type = DatasetChangeMode(mode)
    folder: str | Path | None = image_folder or audio_folder or video_folder
    if folder:
        if image_folder:
            kind = DatasetFolderSourceType.IMAGE
        elif audio_folder:
            kind = DatasetFolderSourceType.AUDIO
        else:
            kind = DatasetFolderSourceType.VIDEO

        folder = Path(folder).absolute()
        # TODO: support desc field
        view.build_from_folder(
            folder,
            kind,
            name=name or folder.name,
            project_uri=project,
            volume_size=volume_size,
            alignment_size=alignment_size,
            auto_label=auto_label,
            mode=mode_type,
            tags=tags,
        )
    elif json_files:
        view.build_from_json_files(
            json_files,
            name=name or f"json-{random_str()}",
            project_uri=project,
            volume_size=volume_size,
            alignment_size=alignment_size,
            field_selector=json_field_selector,
            mode=mode_type,
            tags=tags,
            encoding=file_encoding,
        )
    elif csv_files:
        view.build_from_csv_files(
            csv_files,
            name=name or f"csv-{random_str()}",
            project_uri=project,
            volume_size=volume_size,
            alignment_size=alignment_size,
            mode=mode_type,
            tags=tags,
            dialect=csv_dialect,
            delimiter=csv_delimiter,
            quotechar=csv_quotechar,
            skipinitialspace=csv_skipinitialspace,
            strict=csv_strict,
            encoding=file_encoding,
        )
    elif python_handler:
        _workdir = Path(workdir).absolute()
        config = DatasetConfig(
            name=name or _workdir.name,
            handler=import_object(_workdir, python_handler),
            runtime_uri=runtime,
            project_uri=project,
            desc=desc,
            attr={
                "volume_size": volume_size,
                "alignment_size": alignment_size,
            },
            tags=tags,
        )
        config.do_validate()
        view.build(_workdir, config, mode=mode_type, tags=tags)
    elif hf_repo:
        _candidate_name = (f"{hf_repo}").strip("/").replace("/", "-")
        view.build_from_huggingface(
            hf_repo,
            name=name or _candidate_name,
            project_uri=project,
            volume_size=volume_size,
            alignment_size=alignment_size,
            subsets=hf_subsets,
            split=hf_split,
            revision=hf_revision,
            mode=mode_type,
            cache=hf_cache,
            tags=tags,
            add_info=hf_info,
        )
    else:
        yaml_path = Path(dataset_yaml)
        if not yaml_path.exists():
            raise NotFoundError(yaml_path)

        _workdir = yaml_path.parent
        config = DatasetConfig.create_by_yaml(yaml_path)
        config.name = name or config.name
        config.handler = import_object(_workdir, config.handler)
        config.desc = desc or config.desc
        config.attr = DatasetAttr(
            volume_size=volume_size or config.attr.volume_size,
            alignment_size=alignment_size or config.attr.alignment_size,
        )
        config.runtime_uri = runtime or config.runtime_uri
        config.project_uri = project or config.project_uri
        config.do_validate()
        view.build(_workdir, config, mode=mode_type, tags=tags)


@dataset_cmd.command("diff", help="Dataset version diff")
@click.argument("base_uri", required=True)
@click.argument("compare_uri", required=True)
@click.option(
    "--show-details", is_flag=True, help="Show data different detail by the row"
)
@click.pass_obj
def _diff(
    view: t.Type[DatasetTermView], base_uri: str, compare_uri: str, show_details: bool
) -> None:
    view(base_uri).diff(Resource(compare_uri, typ=ResourceType.dataset), show_details)


@dataset_cmd.command("list", aliases=["ls"])
@click.option(
    "-p",
    "--project",
    default="",
    help="Project URI, the default is the current selected project.",
)
@click.option("-f", "--fullname", is_flag=True, help="Show fullname of dataset version")
@click.option("-sr", "--show-removed", is_flag=True, help="Show removed datasets")
@click.option(
    "--page", type=int, default=DEFAULT_PAGE_IDX, help="Page number for dataset list"
)
@click.option(
    "--size", type=int, default=DEFAULT_PAGE_SIZE, help="Page size for dataset list"
)
@click.option(
    "filters",
    "-fl",
    "--filter",
    multiple=True,
    help="Filter output based on conditions provided.",
)
@click.pass_obj
def _list(
    view: DatasetTermView,
    project: str,
    fullname: bool,
    show_removed: bool,
    page: int,
    size: int,
    filters: list,
) -> None:
    """
    List Dataset of the specified project.

    The filtering flag (-fl or --filter) format is a key=value pair or a flag.
    If there is more than one filter, then pass multiple flags.\n
    (e.g. --filter name=mnist --filter latest)

    \b
    The currently supported filters are:
      name\tTEXT\tThe prefix of the dataset name
      owner\tTEXT\tThe name or id of the dataset owner
      latest\tFLAG\t[Cloud] Only show the latest version
            \t \t[Standalone] Only show the version with "latest" tag
    """
    view.list(project, fullname, show_removed, page, size, filters)


@dataset_cmd.command("info")
@click.argument("dataset")
@click.pass_obj
def _info(view: t.Type[DatasetTermView], dataset: str) -> None:
    """Show dataset details.

    DATASET: argument use the `Dataset URI` format. Version is optional for the Dataset URI.
    If not specified, will show the latest version.

    Example:

        \b
        swcli dataset info mnist # show the latest version of mnist dataset
        swcli dataset info mnist/version/v0 # show the specified version of mnist dataset
        swcli -o json dataset info mnist # show the latest version of mnist dataset in json format
    """
    uri = Resource(dataset, typ=ResourceType.dataset)
    view(uri).info()


@dataset_cmd.command("remove", aliases=["rm"])
@click.argument("dataset")
@click.option(
    "-f",
    "--force",
    is_flag=True,
    help="Force to remove dataset, the removed dataset cannot recover",
)
@click.pass_obj
def _remove(view: t.Type[DatasetTermView], dataset: str, force: bool) -> None:
    """
    Remove dataset

    You can run `swcli dataset recover` to recover the removed datasets.

    DATASET: argument use the `Dataset URI` format, so you can remove the whole dataset or a specified-version dataset.
    """
    click.confirm("continue to remove?", abort=True)
    view(dataset).remove(force)


@dataset_cmd.command("recover")
@click.argument("dataset")
@click.option("-f", "--force", is_flag=True, help="Force to recover dataset")
@click.pass_obj
def _recover(view: t.Type[DatasetTermView], dataset: str, force: bool) -> None:
    """
    Recover dataset

    DATASET: argument use the `Dataset URI` format, so you can recover the whole dataset or a specified-version dataset.
    """
    view(dataset).recover(force)


@dataset_cmd.command("history", help="Show dataset history")
@click.argument("dataset")
@click.option("--fullname", is_flag=True, help="Show version fullname")
@click.pass_obj
def _history(
    view: t.Type[DatasetTermView], dataset: str, fullname: bool = False
) -> None:
    view(dataset).history(fullname)


@dataset_cmd.command("summary", help="Show dataset summary")
@click.argument("dataset")
@click.pass_obj
def _summary(view: t.Type[DatasetTermView], dataset: str) -> None:
    uri = Resource(dataset, typ=ResourceType.dataset)
    view(uri).summary()


@dataset_cmd.command("copy", aliases=["cp"])
@click.argument("src")
@click.argument("dest")
@click.option("-f", "--force", is_flag=True, help="Force copy dataset")
@click.option("-dlp", "--dest-local-project", help="dest local project uri")
@click.option(
    "ignore_tags",
    "-i",
    "--ignore-tag",
    multiple=True,
    help="Ignore tags to copy. The option can be used multiple times.",
)
@optgroup.group(
    "\n **  Copy Mode Selectors",
    cls=MutuallyExclusiveOptionGroup,
    help="The selector of copy mode. If no set, the default is `patch` mode.",
)
@optgroup.option(  # type: ignore
    "-p",
    "--patch",
    "mode",
    flag_value=DatasetChangeMode.PATCH.value,
    default=True,
    help="Patch mode, only update the changed rows and columns for the remote dataset.",
)
@optgroup.option(  # type: ignore
    "-o",
    "--overwrite",
    "mode",
    flag_value=DatasetChangeMode.OVERWRITE.value,
    help="Overwrite mode, update records and delete extraneous rows from the remote dataset.",
)
def _copy(
    src: str,
    dest: str,
    dest_local_project: str,
    mode: str,
    force: bool,
    ignore_tags: t.List[str],
) -> None:
    """
    Copy Dataset between Standalone Instance and Cloud Instance

    SRC: dataset uri with version

    DEST: project uri or dataset uri with name.

    In default, copy dataset with all user custom tags. If you want to ignore some tags, you can use `--ignore-tag` option.
    `latest` and `^v\d+$` are the system builtin tags, they are ignored automatically.

    When the tags are already used for the other dataset version in the dest instance, you should use `--force` option or adjust the tags.

    Example:

        \b
        - copy cloud instance(pre-k8s) mnist project's mnist-cloud dataset to local project(myproject) with a new dataset name 'mnist-local'
            swcli dataset cp cloud://pre-k8s/project/mnist/mnist-cloud/version/ge3tkylgha2tenrtmftdgyjzni3dayq local/project/myproject/mnist-local

        \b
        - copy cloud instance(pre-k8s) mnist project's mnist-cloud dataset to local default project(self) with the cloud instance dataset name 'mnist-cloud'
            swcli dataset cp --patch cloud://pre-k8s/project/dataset/mnist/mnist-cloud/version/ge3tkylgha2tenrtmftdgyjzni3dayq .

        \b
        - copy cloud instance(pre-k8s) mnist project's mnist-cloud dataset to local project(myproject) with the cloud instance dataset name 'mnist-cloud'
            swcli dataset cp cloud://pre-k8s/project/mnist/mnist-cloud/version/ge3tkylgha2tenrtmftdgyjzni3dayq . -dlp myproject

        \b
        - copy cloud instance(pre-k8s) mnist project's mnist-cloud dataset to local default project(self) with a dataset name 'mnist-local'
            swcli dataset cp --overwrite cloud://pre-k8s/project/dataset/mnist/mnist-cloud/version/ge3tkylgha2tenrtmftdgyjzni3dayq mnist-local

        \b
        - copy cloud instance(pre-k8s) mnist project's mnist-cloud dataset to local project(myproject) with a dataset name 'mnist-local'
            swcli dataset cp cloud://pre-k8s/project/mnist/mnist-cloud/version/ge3tkylgha2tenrtmftdgyjzni3dayq mnist-local -dlp myproject

        \b
        - copy standalone instance(local) default project(self)'s mnist-local dataset to cloud instance(pre-k8s) mnist project with a new dataset name 'mnist-cloud'
            swcli dataset cp mnist-local/version/latest cloud://pre-k8s/project/mnist/mnist-cloud

        \b
        - copy standalone instance(local) default project(self)'s mnist-local dataset to cloud instance(pre-k8s) mnist project with standalone instance dataset name 'mnist-local'
            swcli dataset cp mnist-local/version/latest cloud://pre-k8s/project/mnist

        \b
        - copy standalone instance(local) default project(self)'s mnist-local dataset to cloud instance(pre-k8s) mnist project without 'cloud://' prefix
            swcli dataset cp mnist-local/version/latest pre-k8s/project/mnist

        \b
        - copy standalone instance(local) project(myproject)'s mnist-local dataset to cloud instance(pre-k8s) mnist project with standalone instance dataset name 'mnist-local'
            swcli dataset cp local/project/myproject/dataset/mnist-local/version/latest cloud://pre-k8s/project/mnist

        \b
        - copy without some tags
           swcli dataset cp mnist cloud://cloud.starwhale.cn/project/starwhale:public --ignore-tag t1
    """
    DatasetTermView.copy(
        src_uri=src,
        dest_uri=dest,
        mode=DatasetChangeMode(mode),
        dest_local_project_uri=dest_local_project,
        force=force,
        ignore_tags=ignore_tags,
    )


@dataset_cmd.command("tag")
@click.argument("dataset")
@click.argument("tags", nargs=-1)
@click.option("-r", "--remove", is_flag=True, help="Remove tags")
@click.option(
    "-q",
    "--quiet",
    is_flag=True,
    help="Ignore tag name errors like name duplication, name absence",
)
@click.option(
    "-f",
    "--force-add",
    is_flag=True,
    help="force to add tags, even the tag has been used for another version",
)
@click.pass_obj
def _tag(
    view: t.Type[DatasetTermView],
    dataset: str,
    tags: t.List[str],
    remove: bool,
    quiet: bool,
    force_add: bool,
) -> None:
    """Dataset tag management: add, remove and list

    DATASET: argument use the `Dataset URI` format.

    Examples:

        \b
        - list tags of the mnist dataset
        swcli dataset tag mnist

        \b
        - add tags for the mnist dataset
        swcli dataset tag mnist t1 t2
        swcli dataset tag cloud://cloud.starwhale.cn/project/public:starwhale/dataset/mnist/version/latest t1 --force-add
        swcli dataset tag mnist t1 --quiet

        \b
        - remove tags for the mnist dataset
        swcli dataset tag mnist -r t1 t2
        swcli dataset tag cloud://cloud.starwhale.cn/project/public:starwhale/dataset/mnist --remove t1
    """
    view(dataset).tag(
        tags=tags, remove=remove, ignore_errors=quiet, force_add=force_add
    )


@dataset_cmd.command("head")
@click.argument("dataset")
@click.option(
    "-n",
    "--rows",
    default=5,
    show_default=True,
    help="Print the first NUM rows of the dataset",
)
@click.option(
    "-st",
    "--show-types",
    is_flag=True,
    help="Show data types",
)
@click.pass_obj
def _head(
    view: t.Type[DatasetTermView],
    dataset: str,
    rows: int,
    show_types: bool,
) -> None:
    """Print the first n rows of the dataset

    DATASET: argument use the `Dataset URI` format, so you can remove the whole dataset or a specified-version dataset.

    Examples:

        \b
        - print the first 5 rows of the mnist dataset
        swcli dataset head -n 5 mnist

        \b
        - print the data types of the mnist dataset
        swcli dataset head mnist --show-types

        \b
        - print the remote cloud dataset's first 5 rows
        swcli dataset head cloud://cloud-cn/project/test/dataset/mnist -n 5

        \b
        - print the first 5 rows in the json format
        swcli -o json dataset head -n 5 mnist

    """
    view(dataset).head(rows, show_types)
