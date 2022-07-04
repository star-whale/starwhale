import requests


def test_list(base_url, header):
    api_path = "/project"
    res = requests.get(url=base_url + api_path, headers=header)
    response = res.json()

    assert res.status_code == 200
    assert response["code"] == "success"
    assert len(response["data"]["list"]) > 0

    print("Test project list ok.")


def run(base_url, header):
    test_list(base_url, header)



