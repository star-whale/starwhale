import requests


def test_login(base_url):
    api_path = "/login"
    data = {'userName': 'starwhale', 'userPwd': 'abcd1234'}
    res = requests.post(url=base_url + api_path, data=data)
    token = res.headers["Authorization"]

    assert res.status_code == 200
    print("Test login ok.")

    return token
