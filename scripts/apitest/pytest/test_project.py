import requests
import pytest
from common import httputil as hu


class TestProject:

    def test_login(self, host, port):
        hu.login(host, port)

    def test_list(self, host, port):
        res = requests.get(url=hu.url(host, port) + "/project",
                           headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response["code"] == "success"
        assert len(response["data"]["list"]) > 0

        print("Test project list ok.")

    def test_create(self, host, port):
        res = requests.post(url=hu.url(host, port) + "/project",
                            json={"projectName": "project_for_test"},
                            headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response["code"] == "success"

        print("Test Create project ok.")

    def test_remove(self, host, port):
        res = requests.delete(url=hu.url(host, port) + "/project"
                                                       "/project_for_test",
                              headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response["code"] == "success"

        print("Test Remove project ok.")


if __name__ == '__main__':
    pytest.main(["test_project.py"])
