import os
import requests
import time

HEADER_AUTH = 'Authorization'
ENV_ADMIN_TOKEN = 'token'
ENV_TMP_USER_TOKEN = 'tmp_user_token'
ENV_TMP_USER_NAME = 'tmp_user_name'
ENV_TMP_USER_ID = 'tmp_user_id'


def login(host, port):
    api_path = "/login"
    data = {'userName': 'starwhale', 'userPwd': 'abcd1234'}
    res = requests.post(url=url(host, port) + api_path, data=data)

    assert res.status_code == 200

    token = res.headers[HEADER_AUTH]
    os.environ[ENV_ADMIN_TOKEN] = token

    print("Login ok.")


def url(host, port):
    return "http://" + host + ":" + port + "/api/v1"


def header():
    return {HEADER_AUTH: os.getenv(ENV_ADMIN_TOKEN)}


def create_tmp_user(host, port):
    login(host, port)

    api_url = '{api}/user'
    sec = str(int(time.time() * 1000 * 1000))
    user_name = 'pt_user' + sec
    user_password = tmp_user_password()
    os.environ[ENV_TMP_USER_NAME] = user_name
    res = requests.post(url=api_url.format(api=url(host, port)),
                        json={'userName': user_name,
                              'userPwd': user_password},
                        headers=header())
    response = res.json()
    assert res.status_code == 200
    uid = response['data']
    os.environ[ENV_TMP_USER_ID] = uid

    api_url = '{api}/login'
    res = requests.post(url=api_url.format(api=url(host, port)),
                        data={'userName': user_name,
                              'userPwd': user_password})
    assert res.status_code == 200
    token = res.headers[HEADER_AUTH]
    os.environ[ENV_TMP_USER_TOKEN] = token


def tmp_user_destroy(host, port):
    api_url = '{api}/user/{user}/state'
    user_id = os.getenv(ENV_TMP_USER_ID)
    res = requests.put(url=api_url.format(api=url(host, port),
                                          user=user_id),
                       json={'isEnabled': 'false'},
                       headers=header())
    assert res.status_code == 200


def tmp_user_header():
    return {HEADER_AUTH: os.getenv(ENV_TMP_USER_TOKEN)}


def tmp_user_name():
    return os.getenv(ENV_TMP_USER_NAME)


def tmp_user_password():
    return 'abcd'


def tmp_user_id():
    return os.getenv(ENV_TMP_USER_ID)
