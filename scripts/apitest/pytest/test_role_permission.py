import requests
import pytest
from test_role import get_current_user_role
from common import httputil as hu


class TestRolePermission:
    def test_create_user(self, host, port):
        hu.create_tmp_user(host, port)

    def test_get_user_token(self, host, port):
        url = '{api}/user/token/{user}'
        res = requests.get(url=url.format(api=hu.url(host, port),
                                          user=hu.tmp_user_id()),
                           headers=hu.header())
        assert res.status_code == 200

        res = requests.get(url=url.format(api=hu.url(host, port),
                                          user=hu.tmp_user_id()),
                           headers=hu.tmp_user_header())
        assert res.status_code == 403

    def test_update_user_state(self, host, port):
        url = '{api}/user/{user}/state'
        res = requests.put(url=url.format(api=hu.url(host, port),
                                          user=hu.tmp_user_id()),
                           json={'isEnabled': 'true'},
                           headers=hu.tmp_user_header())

        assert res.status_code == 403

        res = requests.put(url=url.format(api=hu.url(host, port),
                                          user=hu.tmp_user_id()),
                           json={'isEnabled': 'true'},
                           headers=hu.header())

        assert res.status_code == 200

    def test_update_user_password(self, host, port):
        url = '{api}/user/{user}/pwd'
        res = requests.put(url=url.format(api=hu.url(host, port),
                                          user=hu.tmp_user_id()),
                           json={'currentUserPwd': hu.tmp_user_password(),
                                 'newPwd': 'abcd'},
                           headers=hu.tmp_user_header())

        assert res.status_code == 403

        res = requests.put(url=url.format(api=hu.url(host, port),
                                          user=hu.tmp_user_id()),
                           json={'currentUserPwd': 'wrong_pwd',
                                 'newPwd': hu.tmp_user_password()},
                           headers=hu.header())

        assert res.status_code == 403

        res = requests.put(url=url.format(api=hu.url(host, port),
                                          user=hu.tmp_user_id()),
                           json={'currentUserPwd': 'abcd1234',
                                 'newPwd': hu.tmp_user_password()},
                           headers=hu.header())

        assert res.status_code == 200

    def test_manage_system_role(self, host, port):
        project_id = '0'
        res = get_current_user_role(host, port, project_id, hu.tmp_user_header())
        response = res.json()

        assert res.status_code == 200
        assert len(response['data']) > 0
        assert response['data'][0]['project']['id'] == project_id
        system_role_id = response['data'][0]['id']

        url = '{api}/role/{role}'
        res = requests.put(url=url.format(api=hu.url(host, port),
                                          role=system_role_id),
                           json={'currentUserPwd': hu.tmp_user_password(),
                                 'roleId': '2'},
                           headers=hu.tmp_user_header())

        assert res.status_code == 403

        res = requests.put(url=url.format(api=hu.url(host, port),
                                          role=system_role_id),
                           json={'currentUserPwd': 'abcd1234',
                                 'roleId': '1'},
                           headers=hu.header())

        assert res.status_code == 200

        res = get_current_user_role(host, port, project_id, hu.tmp_user_header())
        response = res.json()

        assert res.status_code == 200
        assert response['data'][0]['project']['id'] == project_id
        assert response['data'][0]['role']['code'] == 'OWNER'

    def test_destroy(self, host, port):
        hu.tmp_user_destroy(host, port)


if __name__ == '__main__':
    pytest.main(['test_role_permission.py'])
