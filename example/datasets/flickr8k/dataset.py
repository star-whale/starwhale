from typing import List, Tuple, Optional
from html.parser import HTMLParser
from concurrent.futures import wait, ThreadPoolExecutor

import requests

from starwhale import Link, Image, dataset, MIMEType  # noqa: F401
from starwhale.utils.retry import http_retry

PATH_ROOT = "http://hockenmaier.cs.illinois.edu/8k-pictures.html"


@http_retry
def request_link_text(anno_link):
    return requests.get(anno_link, timeout=60).text


class IndexParser(HTMLParser):
    def __init__(self, page: str = PATH_ROOT) -> None:
        super().__init__()
        self.annotations = {}
        self.in_table = False
        self.feed(request_link_text(page))

    def handle_starttag(self, tag: str, attrs: List[Tuple[str, Optional[str]]]) -> None:
        self.current_tag = tag

        if tag == "table":
            self.in_table = True

    def handle_endtag(self, tag: str) -> None:
        self.current_tag = None

        if tag == "table":
            self.in_table = False

    def handle_data(self, data: str) -> None:
        if self.in_table:
            if data == "Image Not Found":
                self.current_img = None
            elif self.current_tag == "a":
                img_id = data
                self.current_img = img_id
                self.annotations[img_id] = []
            elif self.current_tag == "li" and self.current_img:
                img_id = self.current_img
                self.annotations[img_id].append(data.strip())


class FlickrParser(HTMLParser):
    def __init__(self, page: str) -> None:
        super().__init__()
        self.url = ""
        self.width = 0
        self.height = 0
        self.feed(request_link_text(page))

    def handle_starttag(self, tag: str, attrs: List[Tuple[str, Optional[str]]]) -> None:

        if tag == "meta":
            if ("property", "og:image") in attrs:
                for name, value in attrs:
                    if name == "content":
                        self.url = value
            if ("property", "og:image:width") in attrs:
                for name, value in attrs:
                    if name == "content":
                        self.width = int(value)

            if ("property", "og:image:height") in attrs:
                for name, value in attrs:
                    if name == "content":
                        self.height = int(value)


def build_ds():
    ds = dataset("flickr8k")
    idx_parser = IndexParser()
    with ThreadPoolExecutor(max_workers=10) as executor:
        futures = [
            executor.submit(add_img, ds, img, labels)
            for img, labels in idx_parser.annotations.items()
        ]
        wait(futures)

    ds.commit()
    ds.close()


def add_img(ds, img, labels):
    print(f"doing {img}")
    fp = FlickrParser(img)
    ds.append(
        (
            img,
            {
                "image": Image(
                    link=Link(
                        uri=fp.url,
                    ),
                    display_name=img,
                    mime_type=MIMEType.JPEG,
                    shape=(fp.width, fp.height),
                ),
                "labels": labels,
            },
        )
    )


if __name__ == "__main__":
    build_ds()
