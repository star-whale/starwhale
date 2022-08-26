import requests
import pytest
from common import httputil as hu


class TestUser:

    def test_login(self, host, port):
        hu.create_tmp_user(host, port)

    def test_list(self, host, port):
        url = '{api}/user'
        res = requests.get(url=url.format(api=hu.url(host, port)),
                           headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert len(response['data']['list']) > 0

        print('Test User list ok.')

    def test_current_user(self, host, port):
        url = '{api}/user/current'
        res = requests.get(url=url.format(api=hu.url(host, port)),
                           headers=hu.tmp_user_header())
        response = res.json()
        assert res.status_code == 200
        assert response['code'] == 'success'
        assert response['data']['name'] == hu.tmp_user_name()

        print('Test User Current ok.')

    def test_check_current_pwd(self, host, port):
        url = '{api}/user/current/pwd'
        res = requests.post(url=url.format(api=hu.url(host, port)),
                            json={'currentUserPwd': hu.tmp_user_password()},
                            headers=hu.tmp_user_header())
        assert res.status_code == 200

        res = requests.post(url=url.format(api=hu.url(host, port)),
                            json={'currentUserPwd': 'wrong_pwd'},
                            headers=hu.tmp_user_header())
        assert res.status_code == 403

    def test_change_current_pwd(self, host, port):
        url = '{api}/user/current/pwd'
        res = requests.put(url=url.format(api=hu.url(host, port)),
                           json={'currentUserPwd': hu.tmp_user_password(),
                                 'newPwd': 'abcd1'},
                           headers=hu.tmp_user_header())
        assert res.status_code == 200

        url = '{api}/login'
        res = requests.post(url=url.format(api=hu.url(host, port)),
                            data={'userName': hu.tmp_user_name(),
                                  'userPwd': hu.tmp_user_password()})
        assert res.status_code == 401

        res = requests.post(url=url.format(api=hu.url(host, port)),
                            data={'userName': hu.tmp_user_name(),
                                  'userPwd': 'abcd1'})
        assert res.status_code == 200

    def test_destroy(self, host, port):
        hu.tmp_user_destroy(host, port)


if __name__ == '__main__':
    pytest.main(['test_user.py'])
