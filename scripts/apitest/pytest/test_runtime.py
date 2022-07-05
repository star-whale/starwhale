import os
import requests
import pytest
from common import httputil as hu


class TestRuntime:

    def test_login(self, host, port):
        hu.login(host, port)

    def test_list(self, host, port):
        res = requests.get(url=hu.url(host, port) + '/project'
                                                    '/1'
                                                    '/runtime',
                           headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response['code'] == 'success'
        assert response['data']['list'] is not None

        if len(response['data']['list']) > 0:
            swrt_name = response['data']['list'][0]['name']
            os.environ['swrt_name'] = swrt_name

        print('Test Runtime list ok.')

    def test_list_version(self, host, port):
        if os.getenv('swrt_name') is None:
            print('Test Runtime Version list cancel.')
            return
        swrt_name = os.getenv('swrt_name')
        path = '/project/1/runtime/' + swrt_name + '/version'
        res = requests.get(url=hu.url(host, port) + path,
                           headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response['code'] == 'success'
        assert response['data']['list'] is not None
        if len(response['data']['list']) > 0:
            swrt_version_name = response['data']['list'][0]['name']
            os.environ['swrt_version_name'] = swrt_version_name

        print('Test Runtime Version list ok.')

    def test_get_info(self, host, port):
        if os.getenv('swrt_name') is None:
            print('Test Runtime get info cancel.')
            return
        swrt_name = os.getenv('swrt_name')
        res = requests.get(url=hu.url(host, port) + '/project'
                                                    '/1'
                                                    '/runtime'
                                                    '/' + swrt_name,
                           headers=hu.header())
        response = res.json()
        assert res.status_code == 200
        assert response['code'] == 'success'
        assert response['data']['name'] == swrt_name

        print('Test Runtime get info ok.')

    def test_revert(self, host, port):
        swrt_name = os.getenv('swrt_name')
        swrt_version_name = os.getenv('swrt_version_name')
        if swrt_name is None or swrt_version_name is None:
            print('Test Runtime Version revert cancel.')
            return
        path = '/project/1/runtime/' + swrt_name + '/revert'
        res = requests.post(url=hu.url(host, port) + path,
                            json={'versionUrl': swrt_version_name},
                            headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response['code'] == 'success'

        print('Test Runtime Version revert ok.')

    def test_tag_update(self, host, port):
        swrt_name = os.getenv('swrt_name')
        swrt_version_name = os.getenv('swrt_version_name')
        if swrt_name is None or swrt_version_name is None:
            print('Test Runtime Version tag update cancel.')
            return

        path = '/project/1/runtime/' + swrt_name \
               + '/version/' + swrt_version_name + '/tag'

        res = requests.put(url=hu.url(host, port) + path,
                           json={'tag': 'test1', 'action': 'add'},
                           headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response['code'] == 'success'

        res = requests.get(url=hu.url(host, port) + '/project'
                                                    '/1'
                                                    '/runtime'
                                                    '/' + swrt_name,
                           params='versionUrl=' + swrt_version_name,
                           headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response['code'] == 'success'

        print('Test Runtime Version tag update ok.')

    def test_remove(self, host, port):
        if os.getenv('swrt_name') is None:
            print('Test Runtime remove cancel.')
            return
        swrt_name = os.getenv('swrt_name')
        res = requests.delete(url=hu.url(host, port) + '/project'
                                                       '/1'
                                                       '/runtime'
                                                       '/' + swrt_name,
                              headers=hu.header())
        response = res.json()
        assert res.status_code == 200
        assert response['code'] == 'success'

        print('Test Runtime remove ok.')

    def test_recover(self, host, port):
        if os.getenv('swrt_name') is None:
            print('Test Runtime recover cancel.')
            return
        swrt_name = os.getenv('swrt_name')
        path = '/project/1/runtime/' + swrt_name + '/recover'
        res = requests.put(url=hu.url(host, port) + path,
                           headers=hu.header())
        response = res.json()
        assert res.status_code == 200
        assert response['code'] == 'success'

        print('Test Runtime recover ok.')

    def test_head(self, host, port):
        swrt_name = os.getenv('swrt_name')
        swrt_version_name = os.getenv('swrt_version_name')
        if swrt_name is None or swrt_version_name is None:
            print('Test Runtime head cancel.')
            return
        swrt = swrt_name + ':' + swrt_version_name
        res = requests.head(url=hu.url(host, port) + '/project'
                                                     '/runtime',
                            params='project=1&runtime=' + swrt,
                            headers=hu.header())
        assert res.status_code == 200

        print('Test Runtime head ok.')


if __name__ == '__main__':
    pytest.main(['test_runtime.py'])
