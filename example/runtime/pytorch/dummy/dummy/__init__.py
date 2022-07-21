import argparse


def cli():
    parser = argparse.ArgumentParser(description="dummy cli")
    parser.add_argument("--foo", help="foo")
    parser.add_argument("--bar", help="bar")

    args = parser.parse_args()
    print(f"hello from dummy client: {args}")
