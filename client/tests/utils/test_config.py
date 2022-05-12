from starwhale.utils.config import load_swcli_config


def test_load_swcli_config() -> None:
    _config = load_swcli_config()
    assert "controller" in _config
    assert _config["storage"]["root"].endswith(".cache/starwhale") is True
