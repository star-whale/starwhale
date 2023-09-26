import os
from pathlib import Path

from requests_mock import Mocker
from pyfakefs.fake_filesystem_unittest import TestCase

from tests import get_predefined_config_yaml
from starwhale.utils import config as sw_config
from starwhale.consts import DEFAULT_PROJECT
from starwhale.utils.config import (
    SWCliConfigMixed,
    load_swcli_config,
    get_swcli_config_path,
)
from starwhale.base.uri.project import Project
from starwhale.core.project.view import ProjectTermView
from starwhale.core.instance.view import InstanceTermView
from starwhale.core.project.model import StandaloneProject

_existed_config_contents = get_predefined_config_yaml()


class ProjectTestCase(TestCase):
    def setUp(self):
        self.setUpPyfakefs()
        sw_config._config = {}

    def test_project_self(self):
        _config = load_swcli_config()
        assert os.path.exists(os.path.join(_config["storage"]["root"], DEFAULT_PROJECT))

    # TODO: add cloud model
    def test_standalone_model(self):
        _config = load_swcli_config()

        sp = StandaloneProject(uri=Project("local/project/self"))
        ok, reason = sp.create()
        assert not ok
        assert "existed" in reason

        sp = StandaloneProject(uri=Project("local/project/test"))
        ok, reason = sp.create()
        assert ok
        assert "created" in reason
        assert ".starwhale/test" in reason
        assert (
            sp.loc.absolute() == (Path(_config["storage"]["root"]) / "test").absolute()
        )

        info = sp.info()
        assert "test" == info["name"]
        assert ".starwhale/test" in info["location"]

        projects, _ = StandaloneProject.list()
        assert len(projects) == 2
        assert projects[0]["name"] == DEFAULT_PROJECT
        assert projects[1]["name"] == "test"

        ok, _ = sp.remove()
        assert ok
        projects, _ = StandaloneProject.list()
        assert len(projects) == 1
        assert projects[0]["name"] == DEFAULT_PROJECT

        ok, _ = sp.recover()
        assert ok
        projects, _ = StandaloneProject.list()
        assert len(projects) == 2
        assert projects[0]["name"] == DEFAULT_PROJECT
        assert projects[1]["name"] == "test"

    def test_standalone_view_smoke(self):
        for u in ("local/project/test", "test2"):
            ptv = ProjectTermView(u)
            ptv.create()
            ptv.info()
            ptv.remove()
            ptv.recover()
            ProjectTermView.list()

    @Mocker()
    def test_project_select(self, rm: Mocker):
        rm.get(
            "http://1.1.1.2:8182/api/v1/project/new_project", json={"data": {"id": 1}}
        )
        path = get_swcli_config_path()
        self.fs.create_file(path, contents=_existed_config_contents)

        sw = SWCliConfigMixed()
        assert sw.current_instance == "pre-bare"

        InstanceTermView().select("pre-bare2")
        assert sw.current_instance == "pre-bare2"

        ProjectTermView("new_project").select()
        assert sw.current_instance == "pre-bare2"
        assert sw.current_project == "1"
