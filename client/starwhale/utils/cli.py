import typing as t

import click

_TCallCommand = t.Callable[[t.Callable[..., t.Any]], click.Command]
_TCallGroup = t.Callable[[t.Callable[..., t.Any]], click.Group]
_TCallDeco = t.Union[_TCallGroup, _TCallCommand]


class AliasedGroup(click.Group):
    def __init__(self, *args: t.Any, **kwargs: t.Any) -> None:
        super().__init__(*args, **kwargs)
        self._commands: t.Dict[str, t.List[str]] = {}
        self._aliases: t.Dict[str, str] = {}

    def _attach_aliases_args(
        self, orig_deco: _TCallDeco, aliases: t.List[str]
    ) -> _TCallDeco:
        if not aliases:
            return orig_deco

        def alias_cmd_deco(f: t.Callable) -> t.Union[click.Command, click.Group]:
            cmd = orig_deco(f)
            if aliases:
                self._commands[cmd.name] = aliases
                for alias in aliases:
                    self._aliases[alias] = cmd.name
            return cmd

        return alias_cmd_deco

    def command(self, *args: t.Any, **kwargs: t.Any) -> _TCallCommand:
        aliases = kwargs.pop("aliases", [])
        cmd_deco = super().command(*args, **kwargs)
        return self._attach_aliases_args(cmd_deco, aliases)

    def group(self, *args: t.Any, **kwargs: t.Any) -> _TCallGroup:
        aliases = kwargs.pop("aliases", [])
        group_deco = super().group(*args, **kwargs)
        return self._attach_aliases_args(group_deco, aliases)  # type: ignore

    def add_command(
        self,
        cmd: click.Command,
        name: t.Optional[str] = None,
        aliases: t.Optional[t.List[str]] = None,
    ) -> None:
        aliases = aliases or []
        if aliases:
            name = name or cmd.name
            self._commands[name] = aliases
            for alias in aliases:
                self._aliases[alias] = name

        super().add_command(cmd, name)

    def get_command(
        self, ctx: click.Context, cmd_name: str
    ) -> t.Optional[click.Command]:
        rv = click.Group.get_command(self, ctx, cmd_name)
        if rv is not None:
            return rv

        if cmd_name in self._aliases:
            return click.Group.get_command(self, ctx, self._aliases[cmd_name])

        matches = [
            x for x in self.list_commands(ctx) if x.lower().startswith(cmd_name.lower())
        ]

        if not matches:
            return None
        elif len(matches) == 1:
            return click.Group.get_command(self, ctx, matches[0])
        else:
            ctx.fail(f"Too many commands matches: {', '.join(matches)}")

    def resolve_command(
        self, ctx: click.Context, args: t.List[str]
    ) -> t.Tuple[str, click.Command, t.List[str]]:
        _, cmd, args = super().resolve_command(ctx, args)
        # return command name, not alias
        return cmd.name, cmd, args

    def format_commands(
        self, ctx: click.Context, formatter: click.HelpFormatter
    ) -> None:
        commands = []
        for subcommand in self.list_commands(ctx):
            cmd = self.get_command(ctx, subcommand)
            if cmd is None or cmd.hidden:
                continue

            if subcommand in self._commands:
                aliases = ",".join(sorted(self._commands[subcommand]))
                subcommand = f"{subcommand} ({aliases})"
            commands.append((subcommand, cmd))

        if commands:
            limit = formatter.width - 6 - max(len(cmd[0]) for cmd in commands)  # type: ignore
            rows = [
                (subcommand, cmd.get_short_help_str(limit))
                for subcommand, cmd in commands
            ]
            with formatter.section("Commands"):
                formatter.write_dl(rows)
