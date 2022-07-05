import requests
import pytest
from common import httputil as hu


class TestProject:

    def test_login(self, host, port):
        hu.login(host, port)

    def test_list(self, host, port):
        res = requests.get(url=hu.url(host, port) + '/project',
                           headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response['code'] == 'success'
        assert len(response['data']['list']) > 0

        print('Test project list ok.')

    def test_create(self, host, port):
        res = requests.post(url=hu.url(host, port) + '/project',
                            json={'projectName': 'project_for_test'},
                            headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response['code'] == 'success'

        print('Test Project Create ok.')

    def test_info(self, host, port):
        res = requests.get(url=hu.url(host, port) + '/project/project_for_test',
                           headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response['code'] == 'success'
        assert response['data']['name'] == 'project_for_test'

        print('Test Project get information ok.')

    def test_modify(self, host, port):
        res = requests.put(url=hu.url(host, port) + '/project'
                                                    '/project_for_test',
                           json={'projectName': 'project_test_modify'},
                           headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response['code'] == 'success'

        print('Test Project Modify ok.')

    def test_remove(self, host, port):
        res = requests.delete(url=hu.url(host, port) + '/project'
                                                       '/project_test_modify',
                              headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response['code'] == 'success'

        print('Test Project Remove ok.')

    def test_recover(self, host, port):
        res = requests.post(url=hu.url(host, port) + '/project',
                            json={'projectName': 'project_for_recover'},
                            headers=hu.header())
        response = res.json()
        data = response['data']

        requests.delete(url=hu.url(host, port) + '/project'
                                                 '/' + data,
                        headers=hu.header())

        res = requests.put(url=hu.url(host, port) + '/project'
                                                    '/' + data +
                                                    '/recover',
                           headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response['code'] == 'success'

        requests.delete(url=hu.url(host, port) + '/project'
                                                 '/' + data,
                        headers=hu.header())

        print('Test Project Recover ok.')


if __name__ == '__main__':
    pytest.main(['test_project.py'])
