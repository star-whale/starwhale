from __future__ import annotations

import os
import typing as t
from unittest.mock import patch, MagicMock

from requests_mock import Mocker
from pyfakefs.fake_filesystem_unittest import TestCase

from starwhale.consts import HTTPMethod
from starwhale.utils.event import Event
from starwhale.utils.event import event as event_deco
from starwhale.base.client.models.models import EventType


class EventTestCase(TestCase):
    def setUp(self) -> None:
        self.server_alias = "server"
        self.server_url = "http://1.1.1.1"
        self.setUpPyfakefs()

        self.mock_atexit = patch("starwhale.utils.event.atexit", MagicMock())
        self.mock_atexit.start()

    def tearDown(self) -> None:
        _pop = os.environ.pop
        _pop("SW_JOB_VERSION", None)
        _pop("SW_RUN_ID", None)
        _pop("SW_PROJECT_URI", None)
        self.mock_atexit.stop()

    def _get_mock_config(self) -> t.Dict:
        return {
            "current_instance": "local",
            "instances": {
                self.server_alias: {
                    "uri": self.server_url,
                    "current_project": "p",
                    "sw_token": "abcd",
                },
                "local": {"uri": "local", "current_project": "foo"},
            },
            "storage": {"root": "/mock-root/"},
        }

    @Mocker()
    @patch("starwhale.utils.config.load_swcli_config")
    def test_events_workflow(self, mock_req: Mocker, mock_config: MagicMock) -> None:
        mock_config.return_value = self._get_mock_config()
        event = Event()
        assert event.queue.empty()

        add_req = mock_req.request(
            HTTPMethod.POST, f"{self.server_url}/api/v1/project/1/job/2/event"
        )

        os.environ["SW_JOB_VERSION"] = "2"
        os.environ["SW_RUN_ID"] = "3"
        os.environ["SW_PROJECT_URI"] = f"cloud://{self.server_alias}/project/1"

        event.add("t1")
        event.add("t2", {"data": 1})
        event.add("t3", event_type="ERROR")
        event.add(msg="")

        assert event.is_alive()

        event.flush()
        assert event.queue.empty()

        event.close()
        assert not event.is_alive()
        assert event.queue.empty()

        assert add_req.call_count == 3
        assert add_req.request_history[0].path == "/api/v1/project/1/job/2/event"
        assert add_req.request_history[0].json() == {
            "eventType": "INFO",
            "source": "CLIENT",
            "relatedResource": {"eventResourceType": "RUN", "id": 3},
            "message": "t1",
        }
        assert add_req.request_history[1].json() == {
            "eventType": "INFO",
            "source": "CLIENT",
            "relatedResource": {"eventResourceType": "RUN", "id": 3},
            "message": "t2",
            "data": '{"data": 1}',
        }
        assert add_req.request_history[2].json() == {
            "eventType": "ERROR",
            "source": "CLIENT",
            "relatedResource": {"eventResourceType": "RUN", "id": 3},
            "message": "t3",
        }

        add_req.reset()
        del os.environ["SW_JOB_VERSION"]
        with Event() as e:
            e.add("t5")
        assert add_req.call_count == 0

        add_req.reset()
        del os.environ["SW_PROJECT_URI"]
        with Event() as e:
            e.add("t4")
        assert add_req.call_count == 0

    @Mocker()
    @patch("starwhale.utils.config.load_swcli_config")
    def test_event_decorator(self, mock_req: Mocker, mock_config: MagicMock) -> None:
        mock_config.return_value = self._get_mock_config()
        with Event._lock:
            Event._instance = None

        add_req = mock_req.request(
            HTTPMethod.POST, f"{self.server_url}/api/v1/project/1/job/2/event"
        )
        os.environ["SW_JOB_VERSION"] = "2"
        os.environ["SW_RUN_ID"] = "3"
        os.environ["SW_PROJECT_URI"] = f"cloud://{self.server_alias}/project/1"

        def f() -> None:
            ...

        def error_f() -> None:
            raise Exception("error")

        event_deco(msg="t0")(f)
        event_deco(msg="t1")(f)()
        event_deco(msg="t2", external={"data": 1}, event_type="INFO")(f)()
        event_deco(
            msg="t3", external={"data": 2}, event_type=EventType.error, ignore_end=True
        )(f)()

        with self.assertRaisesRegex(Exception, "error"):
            event_deco(msg="t4")(error_f)()

        event_deco(f)()

        assert Event._instance is not None
        assert isinstance(Event._instance, Event)
        Event._instance.flush()

        assert add_req.call_count == 9
        expected = [
            "t1[start]",
            "t1[end]",
            "t2[start]",
            "t2[end]",
            "t3",
            "t4[start]",
            "t4[failed:error]",
            f"func:{f.__qualname__}[start]",
            f"func:{f.__qualname__}[end]",
        ]
        for idx, msg in enumerate(expected):
            assert add_req.request_history[idx].json()["message"] == msg

        assert add_req.request_history[-3].json()["eventType"] == "ERROR"
