import requests
import pytest
from common import httputil as hu


class TestUser:

    def test_login(self, host, port):
        hu.login(host, port)

    def test_list(self, host, port):
        res = requests.get(url=hu.url(host, port) + '/user',
                           headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response['code'] == 'success'
        assert len(response['data']['list']) > 0

        print('Test User list ok.')

    def test_current_user(self, host, port):
        res = requests.get(url=hu.url(host, port) + '/user/current',
                           headers=hu.header())
        response = res.json()
        assert res.status_code == 200
        assert response['code'] == 'success'
        assert response['data']['name'] == 'starwhale'

        print('Test User Current ok.')


if __name__ == '__main__':
    pytest.main(['test_user.py'])
