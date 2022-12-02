import pytest
import requests

from common import httputil as hu


class TestTrash:

    def test_login(self, host, port):
        hu.login(host, port)

    def test_recover_model(self, host, port):
        recover_trash(host, port, 'model')

    def test_recover_dataset(self, host, port):
        recover_trash(host, port, 'dataset')

    def test_recover_runtime(self, host, port):
        recover_trash(host, port, 'runtime')


def recover_trash(host, port, bundle_type):
    res = requests.get(url=hu.url(host, port) + '/project'
                                                '/1'
                                                '/' + bundle_type,
                       headers=hu.header())
    response = res.json()
    if len(response['data']['list']) == 0:
        print('Test Trash recover model  cancel.')
        return

    obj_name = response['data']['list'][0]['name']
    res = requests.delete(url=hu.url(host, port) + '/project'
                                                   '/1'
                                                   '/' + bundle_type +
                              '/' + obj_name,
                          headers=hu.header())
    assert res.status_code == 200

    res = requests.get(url=hu.url(host, port) + '/project'
                                                '/1'
                                                '/trash',
                       headers=hu.header())
    response = res.json()
    assert res.status_code == 200
    assert response['data']['list'] is not None
    assert len(response['data']['list']) > 0
    trash_id = None
    print(response['data']['list'])
    for trash in response['data']['list']:
        if trash['name'] == obj_name:
            trash_id = trash['id']
            break
    assert trash_id is not None
    res = requests.put(url=hu.url(host, port) + '/project'
                                                '/1'
                                                '/trash'
                                                '/' + trash_id,
                       headers=hu.header())
    assert res.status_code == 200

    res = requests.get(url=hu.url(host, port) + '/project'
                                                '/1'
                                                '/' + bundle_type +
                           '/' + obj_name,
                       headers=hu.header())
    response = res.json()
    assert res.status_code == 200
    assert response['code'] == 'success'
    assert response['data']['name'] == obj_name


if __name__ == '__main__':
    pytest.main(['test_trash.py'])
