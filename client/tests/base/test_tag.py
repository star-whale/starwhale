from unittest.mock import patch, MagicMock, PropertyMock

from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.consts import DEFAULT_MANIFEST_NAME
from starwhale.base.tag import StandaloneTag
from starwhale.utils.error import FormatError
from starwhale.utils.config import SWCliConfigMixed
from starwhale.base.uri.resource import Resource, ResourceType


class StandaloneTagTestCase(TestCase):
    def setUp(self) -> None:
        self.setUpPyfakefs()

    @patch("starwhale.utils.config.load_swcli_config")
    @patch(
        "starwhale.utils.config.SWCliConfigMixed._current_instance_obj",
        new_callable=PropertyMock,
    )
    def test_tag_workflow(self, mock_ins: MagicMock, mock_conf: MagicMock) -> None:
        mock_ins.return_value = {
            "type": "standalone",
            "uri": "local",
            "current_project": "foo",
        }
        mock_conf.return_value = {
            "current_instance": "local",
            "instances": {
                "local": {"uri": "local", "current_project": "foo"},
            },
            "storage": {"root": "/root"},
        }
        sw = SWCliConfigMixed()

        st = StandaloneTag(
            Resource(
                "mnist/version/me3dmn3gg4ytanrtmftdgyjzpbrgimi",
                typ=ResourceType.model,
            )
        )

        assert st._manifest_path.name == DEFAULT_MANIFEST_NAME
        assert (
            st._manifest_path
            == sw.rootdir / "foo" / "model" / "mnist" / DEFAULT_MANIFEST_NAME
        )

        _manifest = st._get_manifest()
        assert len(_manifest["tags"]) == 0
        assert len(_manifest["versions"]) == 0

        st.add(["test", "latest", "me3"])
        _manifest = st._get_manifest()
        _version = "me3dmn3gg4ytanrtmftdgyjzpbrgimi"
        assert _manifest["name"] == "mnist"
        assert _manifest["typ"] == "model"
        assert _manifest["tags"]["latest"] == _version
        assert _manifest["tags"]["test"] == _version
        assert _manifest["versions"][_version]["latest"]
        assert _manifest["versions"][_version]["test"]

        assert set(st.list()) == {"test", "latest", "me3"}

        st.remove(["latest", "notfound"], ignore_errors=True)
        assert set(st.list()) == {"test", "me3"}
        _manifest = st._get_manifest()
        assert "latest" not in _manifest["tags"]
        assert _manifest["versions"][_version].get("test")
        assert not _manifest["versions"][_version].get("latest")

        st = StandaloneTag(
            Resource(
                "mnist/version/gnstmntggi4tinrtmftdgyjzo5wwy2y",
                typ=ResourceType.model,
            )
        )
        st.add(["latest", "test2", "test"])
        assert set(st.list()) == {"latest", "test2", "test"}

        _manifest = st._get_manifest()
        assert set(_manifest["tags"]) == {"latest", "test", "test2", "me3"}
        assert _manifest["tags"]["test"] == "gnstmntggi4tinrtmftdgyjzo5wwy2y"
        assert _manifest["tags"]["me3"] == "me3dmn3gg4ytanrtmftdgyjzpbrgimi"
        assert _manifest["tags"]["latest"] == "gnstmntggi4tinrtmftdgyjzo5wwy2y"
        assert _manifest["tags"]["test2"] == "gnstmntggi4tinrtmftdgyjzo5wwy2y"
        assert _manifest["versions"]["me3dmn3gg4ytanrtmftdgyjzpbrgimi"]["me3"]
        assert _manifest["versions"]["gnstmntggi4tinrtmftdgyjzo5wwy2y"]["test"]

    @patch(
        "starwhale.utils.config.SWCliConfigMixed._current_instance_obj",
        new_callable=PropertyMock,
    )
    def test_auto_fast_tag(self, mock_ins: MagicMock) -> None:
        mock_ins.return_value = {"type": "standalone", "uri": "local"}
        version = "me3dmn3gg4ytanrtmftdgyjzpbrgimi"
        st = StandaloneTag(Resource(f"mnist/version/{version}", typ=ResourceType.model))
        assert st._get_manifest()["fast_tag_seq"] == -1
        st.add_fast_tag()
        assert st._get_manifest()["fast_tag_seq"] == 0
        st.add_fast_tag()
        st.add_fast_tag()
        st.add_fast_tag()
        assert st._get_manifest()["fast_tag_seq"] == 3
        assert st._get_manifest()["tags"]["v0"] == version
        assert st._get_manifest()["tags"]["v3"] == version
        st.add(["v4", "v5", "v6"])
        st.add_fast_tag()
        assert st._get_manifest()["fast_tag_seq"] == 7
        assert st._get_manifest()["tags"]["v7"] == version

        st.remove(["v7", "v6"], ignore_errors=True)
        st.add_fast_tag()
        assert st._get_manifest()["fast_tag_seq"] == 8
        assert st._get_manifest()["tags"]["v8"] == version

        assert st.list() == ["latest", "v0", "v1", "v2", "v3", "v4", "v5", "v8"]

    def test_check_tags_validation(self) -> None:
        _chk = StandaloneTag.check_tags_validation

        _chk(["t0", " t1 ", "v0abc"])

        invalid_obj_tags = [[""], ["1"], ["$#"], ["a" * 100]]

        for tags in invalid_obj_tags:
            with self.assertRaises(FormatError):
                _chk(tags)

        with self.assertRaisesRegex(FormatError, "tag:latest is builtin"):
            _chk(["latest"])

        with self.assertRaisesRegex(FormatError, "tag:v0 is auto-incremental"):
            _chk(["v0"])
