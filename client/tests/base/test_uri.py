from starwhale.base.type import InstanceType, URIType
from starwhale.base.uri import URI

from pyfakefs.fake_filesystem_unittest import TestCase
from starwhale.utils import config as sw_config
from starwhale.utils.config import get_swcli_config_path

from .. import get_predefined_config_yaml


_existed_config_contents = get_predefined_config_yaml()


class URITestCase(TestCase):
    def setUp(self):
        self.setUpPyfakefs()
        sw_config._config = {}
        path = get_swcli_config_path()
        self.fs.create_file(path, contents=_existed_config_contents)

    def test_uri(self):
        ts = {
            "": {
                "instance": "http://1.1.1.1:8182",
                "instance_type": "cloud",
                "project": "self",
                "obj_typ": "unknown",
                "obj_name": "",
                "obj_version": "",
                "full_uri": "http://1.1.1.1:8182/project/self",
                "real_request_uri": "http://1.1.1.1:8182/api/v1/project/self",
            },
            "/model/mm/version/meydczbrmi2ggnrtmftdgyjzpfuto6q": {
                "instance": "http://1.1.1.1:8182",
                "instance_type": "cloud",
                "project": "self",
                "obj_typ": "model",
                "obj_name": "mm",
                "obj_version": "meydczbrmi2ggnrtmftdgyjzpfuto6q",
                "full_uri": "http://1.1.1.1:8182/project/self/model/mm/version/meydczbrmi2ggnrtmftdgyjzpfuto6q",
                "real_request_uri": "http://1.1.1.1:8182/api/v1/project/self/model/mm/version/meydczbrmi2ggnrtmftdgyjzpfuto6q",
            },
            "http://10.131.0.2:8182/project/mnist/model/mm/version/meydczbrmi2ggnrtmftdgyjzpfuto6q": {
                "instance": "http://10.131.0.2:8182",
                "instance_type": "cloud",
                "project": "mnist",
                "obj_typ": "model",
                "obj_name": "mm",
                "obj_version": "meydczbrmi2ggnrtmftdgyjzpfuto6q",
                "real_request_uri": "http://10.131.0.2:8182/api/v1/project/mnist/model/mm/version/meydczbrmi2ggnrtmftdgyjzpfuto6q",
            },
            "http://10.131.0.2:8182/project/mnist/": {
                "instance": "http://10.131.0.2:8182",
                "instance_type": "cloud",
                "project": "mnist",
                "obj_typ": "unknown",
                "obj_name": "",
                "obj_version": "",
                "real_request_uri": "http://10.131.0.2:8182/api/v1/project/mnist",
            },
            "local/project/mnist/runtime/rt": {
                "instance": "local",
                "instance_type": "standalone",
                "project": "mnist",
                "obj_typ": "runtime",
                "obj_name": "rt",
                "obj_version": "",
                "real_request_uri": ".cache/starwhale/mnist/runtime/rt",
            },
            "local/project/mnist/dataset/dd/version/meydczbrmi2ggnrtmftdgyjzpfuto6q": {
                "instance": "local",
                "instance_type": "standalone",
                "project": "mnist",
                "obj_typ": "dataset",
                "obj_name": "dd",
                "obj_version": "meydczbrmi2ggnrtmftdgyjzpfuto6q",
                "real_request_uri": ".cache/starwhale/mnist/dataset/dd/me/meydczbrmi2ggnrtmftdgyjzpfuto6q",
            },
            "cloud://pre-base/project/mnist/model/mm/version/meydczbrmi2ggnrtmftdgyjzpfuto6q": {
                "instance": "http://1.1.1.1:8182",
                "instance_type": "cloud",
                "project": "mnist",
                "obj_typ": "model",
                "obj_name": "mm",
                "obj_version": "meydczbrmi2ggnrtmftdgyjzpfuto6q",
                "full_uri": "http://1.1.1.1:8182/project/mnist/model/mm/version/meydczbrmi2ggnrtmftdgyjzpfuto6q",
                "real_request_uri": "http://1.1.1.1:8182/api/v1/project/mnist/model/mm/version/meydczbrmi2ggnrtmftdgyjzpfuto6q",
            },
            "project/mnist/model/mm/version/meydczbrmi2ggnrtmftdgyjzpfuto6q": {
                "instance": "http://1.1.1.1:8182",
                "instance_type": "cloud",
                "project": "mnist",
                "obj_typ": "model",
                "obj_name": "mm",
                "obj_version": "meydczbrmi2ggnrtmftdgyjzpfuto6q",
                "full_uri": "http://1.1.1.1:8182/project/mnist/model/mm/version/meydczbrmi2ggnrtmftdgyjzpfuto6q",
                "real_request_uri": "http://1.1.1.1:8182/api/v1/project/mnist/model/mm/version/meydczbrmi2ggnrtmftdgyjzpfuto6q",
            },
        }

        for k, v in ts.items():
            uri = URI(k)
            assert uri.instance == v["instance"]
            assert uri.project == v["project"]
            assert uri.object.typ == v["obj_typ"]
            assert uri.object.name == v["obj_name"]
            assert uri.object.version == v["obj_version"]
            assert uri.full_uri == v.get("full_uri", "").strip(
                "/"
            ) or uri.full_uri == k.strip("/")
            assert uri.instance_type == v["instance_type"]
            assert str(uri.real_request_uri) == v["real_request_uri"] or str(
                uri.real_request_uri
            ).endswith(v["real_request_uri"])

    def test_expected_uri(self):
        uri = URI("test", expected_type=URIType.PROJECT)
        assert uri.instance == "http://1.1.1.1:8182"
        assert uri.instance_type == InstanceType.CLOUD
        assert uri.project == "test"

        uri = URI("project_for_test1", expected_type=URIType.PROJECT)
        assert uri.instance == "http://1.1.1.1:8182"
        assert uri.instance_type == InstanceType.CLOUD
        assert uri.project == "project_for_test1"

        uri = URI("test", expected_type=URIType.MODEL)
        assert uri.instance == "http://1.1.1.1:8182"
        assert uri.instance_type == InstanceType.CLOUD
        assert uri.project == "self"
        assert uri.object.name == "test"
        assert uri.object.typ == URIType.MODEL
        assert uri.object.version == ""

        uri = URI("", expected_type=URIType.INSTANCE)
        assert uri.instance == "http://1.1.1.1:8182"
        assert uri.project == "self"
        assert uri.object.name == ""

        uri = URI("pre-k8s", expected_type=URIType.INSTANCE)
        assert uri.instance == "pre-k8s"
        assert uri.project == "self"
        assert uri.object.name == ""

        uri = URI("", expected_type=URIType.PROJECT)
        assert uri.instance == "http://1.1.1.1:8182"
        assert uri.project == "self"
        assert uri.object.name == ""

        uri = URI("http://12.2.2.2:8080/project/test2", expected_type=URIType.PROJECT)
        assert uri.instance == "http://12.2.2.2:8080"
        assert uri.project == "test2"
        assert uri.object.name == ""

        uri = URI("mnist/version/g4zwkyjumm2d", expected_type=URIType.RUNTIME)
        assert uri.object.name == "mnist"
        assert uri.object.typ == URIType.RUNTIME
        assert uri.object.version == "g4zwkyjumm2d"
