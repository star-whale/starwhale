
def add_mngt_command(cli):
    @cli.command("login", help="Login remote StarWhale Controller")
    def _login():
        pass


    @cli.command("logout", help="Logout StarWhale Controller")
    def _logout():
        pass


    @cli.command("quickstart", help="StarWhale Quickstart")
    def _quickstart():
        pass


    @cli.command("autocomplete", help="Generate zsh/bash command auto complete")
    def _autocompete():
        pass

