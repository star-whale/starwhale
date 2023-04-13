import csv
import sys
from io import StringIO

import requests

from starwhale import Link, Image, dataset, MIMEType  # noqa: F401
from starwhale.utils.retry import http_retry

PATH_ROOT = "https://raw.githubusercontent.com/zhenbench/z-bench/main"


@http_retry
def request_link_text(index_link):
    return requests.get(index_link, timeout=10).text


def build_ds(ds_name: str) -> None:
    ds = dataset(f"z_bench_{ds_name}")
    csv_reader = csv.reader(
        StringIO(request_link_text(f"{PATH_ROOT}/{ds_name}.samples.csv")), delimiter=","
    )
    line_count = 0
    for row in csv_reader:
        if line_count == 0:
            print(f'Column names are {", ".join(row)}')
            line_count += 1
        else:
            ds.append(
                {
                    "prompt": row[0],
                    "task_type": row[1],
                    "ref_answer": row[2],
                    "gpt3d5": row[3],
                    "gpt4": row[4],
                }
            )

    ds.commit()
    ds.close()


if __name__ == "__main__":
    build_ds(sys.argv[1])
