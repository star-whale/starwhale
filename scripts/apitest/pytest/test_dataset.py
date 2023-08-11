import os

import pytest
import requests
from common import httputil as hu


class TestDataset:
    def test_login(self, host, port):
        hu.login(host, port)

    def test_list(self, host, port):
        res = requests.get(
            url=hu.url(host, port) + "/project/1/dataset", headers=hu.header()
        )
        response = res.json()

        assert res.status_code == 200
        assert response["code"] == "success"
        assert response["data"]["list"] is not None

        if len(response["data"]["list"]) > 0:
            swds_name = response["data"]["list"][0]["name"]
            os.environ["swds_name"] = swds_name

        print("Test Dataset list ok.")

    def test_list_version(self, host, port):
        if os.getenv("swds_name") is None:
            print("Test Dataset Version list cancel.")
            return
        swds_name = os.getenv("swds_name")
        path = f"/project/1/dataset/{swds_name}/version"
        res = requests.get(url=hu.url(host, port) + path, headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response["code"] == "success"
        assert response["data"]["list"] is not None
        if len(response["data"]["list"]) > 0:
            swds_version_name = response["data"]["list"][0]["name"]
            os.environ["swds_version_name"] = swds_version_name

        print("Test Dataset Version list ok.")

    def test_get_info(self, host, port):
        if os.getenv("swds_name") is None:
            print("Test Dataset get info cancel.")
            return
        swds_name = os.getenv("swds_name")
        res = requests.get(
            url=hu.url(host, port) + f"/project/1/dataset/{swds_name}",
            headers=hu.header(),
        )
        response = res.json()
        assert res.status_code == 200
        assert response["code"] == "success"
        assert response["data"]["name"] == swds_name

        print("Test Dataset get info ok.")

    def test_revert(self, host, port):
        swds_name = os.getenv("swds_name")
        swds_version_name = os.getenv("swds_version_name")
        if swds_name is None or swds_version_name is None:
            print("Test Dataset Version revert cancel.")
            return
        path = "/project/1/dataset/" + swds_name + "/revert"
        res = requests.post(
            url=hu.url(host, port) + path,
            json={"versionUrl": swds_version_name},
            headers=hu.header(),
        )
        response = res.json()

        assert res.status_code == 200
        assert response["code"] == "success"

        print("Test Dataset Version revert ok.")

    def test_tag_update(self, host, port):
        swds_name = os.getenv("swds_name")
        swds_version_name = os.getenv("swds_version_name")
        if swds_name is None or swds_version_name is None:
            print("Test Dataset Version tag update cancel.")
            return

        path = f"/project/1/dataset/{swds_name}/version/{swds_version_name}/tag"
        res = requests.post(
            url=hu.url(host, port) + path,
            json={"tag": "test1"},
            headers=hu.header(),
        )
        response = res.json()

        assert res.status_code == 200
        assert response["code"] == "success"

        res = requests.get(
            url=hu.url(host, port) + path,
            headers=hu.header(),
        )
        response = res.json()

        assert res.status_code == 200
        assert "test1" in response["data"]

        res = requests.delete(hu.url(host, port) + path + "/test1", headers=hu.header())
        assert res.status_code == 200

        print("Test Dataset Version tag update ok.")

    def test_head(self, host, port):
        swds_name = os.getenv("swds_name")
        swds_version_name = os.getenv("swds_version_name")
        if swds_name is None or swds_version_name is None:
            print("Test Dataset head cancel.")
            return
        res = requests.head(
            url=hu.url(host, port)
            + f"/project/1/dataset/{swds_name}/version/{swds_version_name}",
            headers=hu.header(),
        )
        assert res.status_code == 200

        print("Test Dataset head ok.")


if __name__ == "__main__":
    pytest.main(["test_dataset.py"])
