import os
from pathlib import Path

from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.core.project.model import StandaloneProject
from starwhale.core.project.view import ProjectTermView
from starwhale.base.uri import URI
from starwhale.utils import config as sw_config
from starwhale.utils.config import load_swcli_config
from starwhale.consts import DEFAULT_PROJECT


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

        sp = StandaloneProject(uri=URI("local/project/self"))
        ok, reason = sp.create()
        assert not ok
        assert "existed" in reason

        sp = StandaloneProject(uri=URI("local/project/test"))
        ok, reason = sp.create()
        assert ok
        assert "created" in reason
        assert ".cache/starwhale/test" in reason
        assert (
            sp.loc.absolute() == (Path(_config["storage"]["root"]) / "test").absolute()
        )

        info = sp.info()
        assert "test" == info["name"]
        assert ".cache/starwhale/test" in info["location"]

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
