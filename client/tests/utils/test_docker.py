import os
import getpass as gt
from pwd import getpwnam
from unittest import TestCase

from starwhale.utils import config
from starwhale.utils.docker import gen_swcli_docker_cmd


class TestDocker(TestCase):
    def test_gen_cmd(self) -> None:
        cmd = gen_swcli_docker_cmd("image1")
        self.assertTrue(cmd.startswith("docker run"))
        args = cmd.split()
        pwd = os.getcwd()
        self.assertTrue("-w" in args)
        self.assertTrue(pwd in args)
        self.assertTrue(f"{pwd}:{pwd}" in args)
        self.assertTrue("image1" in args)
        self.assertTrue(f"SW_USER={gt.getuser()}" in args)
        self.assertTrue(f"SW_USER_ID={getpwnam(gt.getuser()).pw_uid}" in args)
        self.assertTrue("SW_USER_GROUP_ID=0" in args)
        rootdir = config.load_swcli_config()["storage"]["root"]
        self.assertTrue(f"SW_LOCAL_STORAGE={rootdir}" in args)
        self.assertTrue(f"{rootdir}:{rootdir}" in args)
        config_path = config.get_swcli_config_path()
        self.assertTrue(f"SW_CLI_CONFIG={config_path}" in args)
        self.assertTrue(f"{config_path}:{config_path}" in args)
