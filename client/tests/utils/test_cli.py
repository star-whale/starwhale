from unittest import TestCase
from unittest.mock import MagicMock

import click

from starwhale.utils.cli import AliasedGroup


class AliasedGroupTestCase(TestCase):
    def test_workflow(self) -> None:
        @click.group("group", cls=AliasedGroup)
        def _group() -> None:
            ...

        @_group.group("subgroup", aliases=["subgrp"])
        def _sub_group() -> None:
            ...

        @_group.command("command", aliases=["cmd"])
        def _cmd() -> None:
            ...

        def _create_cli() -> click.core.Group:
            @click.group(cls=AliasedGroup)
            def _cli() -> None:
                ...

            _cli.add_command(_group, aliases=["grp", "grp_another"])
            return _cli

        cli = _create_cli()

        assert cli._commands["group"] == ["grp", "grp_another"]
        assert cli._aliases["grp"] == "group"
        assert cli._aliases["grp_another"] == "group"

        ctx = click.Context(cli)
        group_cmd = cli.get_command(ctx, "group")
        grp_cmd = cli.get_command(ctx, "grp")
        grp_another_cmd = cli.get_command(ctx, "grp_another")
        short_group_cmd = cli.get_command(ctx, "g")

        assert group_cmd is grp_cmd
        assert grp_cmd.name == "group"
        assert short_group_cmd is group_cmd
        assert short_group_cmd.name == "group"
        assert grp_another_cmd.name == "group"

        not_found_cmd = cli.get_command(ctx, "notfound")
        assert not_found_cmd is None

        assert group_cmd._commands["subgroup"] == ["subgrp"]
        assert group_cmd._commands["command"] == ["cmd"]
        assert group_cmd._aliases["cmd"] == "command"

        cmd = group_cmd.get_command(ctx, "command")
        cmd_alias = group_cmd.get_command(ctx, "cmd")
        assert cmd is cmd_alias
        assert cmd.name == "command"
        assert cmd_alias.name == "command"

        formatter = MagicMock(width=20)
        cli.format_commands(ctx, formatter)

        assert formatter.write_dl.call_args[0][0][0][0] == "group (grp,grp_another)"

        formatter.reset_mock()
        group_cmd.format_commands(ctx, formatter)
        assert formatter.write_dl.call_args[0][0][0][0] == "command (cmd)"
        assert formatter.write_dl.call_args[0][0][1][0] == "subgroup (subgrp)"
