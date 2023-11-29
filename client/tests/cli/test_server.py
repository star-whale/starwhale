from unittest.mock import patch, MagicMock

from click.testing import CliRunner
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.utils import load_yaml
from starwhale.cli.server import stop as server_stop
from starwhale.cli.server import start as server_start
from starwhale.cli.server import status as server_status
from starwhale.cli.server import _TEMPLATE_DIR
from starwhale.utils.config import SWCliConfigMixed


class ServerTestCase(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()
        self.compose_yaml_path = SWCliConfigMixed().server_compose

    @patch("starwhale.cli.server.check_call")
    def test_start(self, m_call: MagicMock) -> None:
        assert not self.compose_yaml_path.exists()
        self.fs.add_real_directory(_TEMPLATE_DIR)
        runner = CliRunner()
        result = runner.invoke(
            server_start,
            [
                "--host",
                "0.0.0.0",
                "--port",
                "8083",
                "--detach",
                "--dry-run",
            ],
            obj=MagicMock(),
        )
        assert result.exit_code == 0
        assert "render compose yaml file" in result.output
        assert "visit web: http://0.0.0.0:8083" in result.output
        assert m_call.call_args[0][0] == [
            "docker",
            "compose",
            "-f",
            str(self.compose_yaml_path),
            "up",
            "--detach",
            "--dry-run",
        ]

        assert self.compose_yaml_path.exists()
        compose_content = load_yaml(self.compose_yaml_path)
        assert "db" in compose_content["services"]

        server_content = compose_content["services"]["server"]
        assert server_content["ports"] == [
            {
                "host_ip": "0.0.0.0",
                "mode": "host",
                "protocol": "tcp",
                "published": 8082,
                "target": 8083,
            }
        ]
        assert (
            server_content["image"]
            == "docker-registry.starwhale.cn/star-whale/server:latest"
        )
        assert (
            "SW_DOCKER_CONTAINER_NETWORK=starwhale_local_ns"
            in server_content["environment"]
        )

    @patch("starwhale.cli.server.check_call")
    def test_stop(self, m_call: MagicMock) -> None:
        result = CliRunner().invoke(server_stop, obj=MagicMock())
        assert result.exit_code == 0
        assert m_call.call_args[0][0] == [
            "docker",
            "compose",
            "-f",
            str(self.compose_yaml_path),
            "stop",
        ]

    @patch("starwhale.cli.server.check_call")
    def test_status(self, m_call: MagicMock) -> None:
        result = CliRunner().invoke(server_status, obj=MagicMock())
        assert result.exit_code == 0
        assert m_call.call_args[0][0] == [
            "docker",
            "compose",
            "-f",
            str(self.compose_yaml_path),
            "ps",
        ]
