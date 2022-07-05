import os
import requests


def login(host, port):
    api_path = "/login"
    data = {'userName': 'starwhale', 'userPwd': 'abcd1234'}
    res = requests.post(url=url(host, port) + api_path, data=data)

    assert res.status_code == 200

    token = res.headers["Authorization"]
    os.environ['token'] = token


def url(host, port):
    return "http://" + host + ":" + port + "/api/v1"


def header():
    return {'Authorization': os.getenv('token')}
