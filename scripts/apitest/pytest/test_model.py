import os

import pytest
import requests
from common import httputil as hu


class TestModel:
    def test_login(self, host, port):
        hu.login(host, port)

    def test_list(self, host, port):
        res = requests.get(
            url=hu.url(host, port) + "/project/1/model", headers=hu.header()
        )
        response = res.json()

        assert res.status_code == 200
        assert response["code"] == "success"
        assert response["data"]["list"] is not None

        if len(response["data"]["list"]) > 0:
            swmp_name = response["data"]["list"][0]["name"]
            os.environ["swmp_name"] = swmp_name

        print("Test Model list ok.")

    def test_list_version(self, host, port):
        if os.getenv("swmp_name") is None:
            print("Test Model Version list cancel.")
            return
        swmp_name = os.getenv("swmp_name")
        path = f"/project/1/model/{swmp_name}/version"
        res = requests.get(url=hu.url(host, port) + path, headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response["code"] == "success"
        assert response["data"]["list"] is not None
        if len(response["data"]["list"]) > 0:
            swmp_version_name = response["data"]["list"][0]["name"]
            os.environ["swmp_version_name"] = swmp_version_name

        print("Test Model Version list ok.")

    def test_get_info(self, host, port):
        if os.getenv("swmp_name") is None:
            print("Test Model get info cancel.")
            return
        swmp_name = os.getenv("swmp_name")
        res = requests.get(
            url=hu.url(host, port) + f"/project/1/model/{swmp_name}",
            headers=hu.header(),
        )
        response = res.json()
        assert res.status_code == 200
        assert response["code"] == "success"
        assert response["data"]["name"] == swmp_name

        print("Test Model get info ok.")

    def test_revert(self, host, port):
        swmp_name = os.getenv("swmp_name")
        swmp_version_name = os.getenv("swmp_version_name")
        if swmp_name is None or swmp_version_name is None:
            print("Test Model Version revert cancel.")
            return
        path = f"/project/1/model/{swmp_name}/revert"
        res = requests.post(
            url=hu.url(host, port) + path,
            json={"versionUrl": swmp_version_name},
            headers=hu.header(),
        )
        response = res.json()

        assert res.status_code == 200
        assert response["code"] == "success"

        print("Test Model Version revert ok.")

    def test_tag_update(self, host, port):
        swmp_name = os.getenv("swmp_name")
        swmp_version_name = os.getenv("swmp_version_name")
        if swmp_name is None or swmp_version_name is None:
            print("Test Model Version tag update cancel.")
            return

        path = f"/project/1/model/{swmp_name}/version/{swmp_version_name}/tag"

        res = requests.post(
            url=hu.url(host, port) + path,
            json={"tag": "test1"},
            headers=hu.header(),
        )
        response = res.json()

        assert res.status_code == 200

        res = requests.get(
            url=hu.url(host, port) + path,
            headers=hu.header(),
        )
        response = res.json()

        assert res.status_code == 200
        assert "test1" in response["data"]

        res = requests.delete(hu.url(host, port) + path + "/test1", headers=hu.header())
        assert res.status_code == 200

        print("Test Model Version tag update ok.")

    def test_head(self, host, port):
        swmp_name = os.getenv("swmp_name")
        swmp_version_name = os.getenv("swmp_version_name")
        if swmp_name is None or swmp_version_name is None:
            print("Test Model head cancel.")
            return
        res = requests.head(
            url=hu.url(host, port)
            + f"/project/1/model/{swmp_name}/version/{swmp_version_name}",
            headers=hu.header(),
        )
        assert res.status_code == 200

        print("Test Model head ok.")


if __name__ == "__main__":
    pytest.main(["test_model.py"])
