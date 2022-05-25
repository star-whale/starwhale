import click

from .cmd import bootstrap_cmd


def create_sw_bootstrap() -> click.core.Group:
    @click.group()
    def bs() -> None:
        print("hello! you are using starwhale bootstrap to deploy cluster")

    bs.add_command(bootstrap_cmd)
    return bs


bootstrap = create_sw_bootstrap()
if __name__ == "__main__":
    bootstrap()
