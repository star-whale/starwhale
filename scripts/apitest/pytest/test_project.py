import os
import time

import pytest
import requests

from common import httputil as hu


def get_project(host, port, name):
    url = '{api}/project/{project}'
    res = requests.get(url=url.format(api=hu.url(host, port), project=name),
                       headers=hu.header())
    return res


class TestProject:
    def test_login(self, host, port):
        hu.login(host, port)

    def test_list(self, host, port):
        url = '{api}/project'
        res = requests.get(url=url.format(api=hu.url(host, port)),
                           headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response['code'] == 'success'
        assert len(response['data']['list']) > 0

        print('Test project list ok.')

    def test_create(self, host, port):
        url = '{api}/project'
        sec = str(int(time.time()))
        project_name = 'pytest' + sec
        os.environ['project_name'] = project_name
        res = requests.post(url=url.format(api=hu.url(host, port)),
                            json={'projectName': project_name,
                                  'privacy': 'public',
                                  'ownerId': 1,
                                  'description': 'project for pytest'},
                            headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response['code'] == 'success'

        print('Test Project Create ok.')

    def test_info(self, host, port):
        project_name = os.getenv('project_name')
        res = get_project(host, port, project_name)
        response = res.json()

        assert res.status_code == 200
        assert response['code'] == 'success'
        assert response['data']['name'] == project_name
        assert response['data']['privacy'] == 'PUBLIC'
        assert response['data']['description'] == 'project for pytest'

        print('Test Project get information ok.')

    def test_project_role(self, host, port):
        url = '{api}/project/{project}/role'
        project_name = os.getenv('project_name')
        res = requests.get(url=url.format(api=hu.url(host, port),
                                          project=project_name),
                           headers=hu.header())
        response = res.json()
        assert res.status_code == 200
        assert len(response['data']) > 0

    def test_modify(self, host, port):
        url = '{api}/project/{project}'
        project_name = os.getenv('project_name')
        project_name_modified = project_name + "_modified"
        res = requests.put(url=url.format(api=hu.url(host, port),
                                          project=project_name),
                           json={'projectName': project_name_modified,
                                 'privacy': 'private',
                                 'description': 'modified description'},
                           headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response['code'] == 'success'

        res = get_project(host, port, project_name_modified)
        response = res.json()

        assert res.status_code == 200
        assert response['code'] == 'success'
        assert response['data']['name'] == project_name_modified
        assert response['data']['privacy'] == 'PRIVATE'
        assert response['data']['description'] == 'modified description'

        print('Test Project Modify ok.')

    def test_remove(self, host, port):
        url = '{api}/project/{project}'
        project_name_modified = os.getenv('project_name') + "_modified"
        res = requests.delete(url=url.format(api=hu.url(host, port),
                                             project=project_name_modified),
                              headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response['code'] == 'success'

        res = get_project(host, port, project_name_modified)
        assert res.status_code == 404 or res.status_code == 400

        print('Test Project Remove ok.')

    def test_recover(self, host, port):
        url = '{api}/project'
        project_name_recover = os.getenv('project_name') + "_recover"
        res = requests.post(url=url.format(api=hu.url(host, port)),
                            json={'projectName': project_name_recover,
                                  'privacy': 'private',
                                  'ownerId': 1,
                                  'description': ''},
                            headers=hu.header())
        response = res.json()
        pid = response['data']

        url = '{api}/project/{project}'
        requests.delete(url=url.format(api=hu.url(host, port),
                                       project=pid),
                        headers=hu.header())

        url = '{api}/project/{project}/recover'
        res = requests.put(url=url.format(api=hu.url(host, port),
                                          project=pid),
                           headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response['code'] == 'success'

        res = get_project(host, port, pid)
        assert res.status_code == 200

        url = '{api}/project/{project}'
        requests.delete(url=url.format(api=hu.url(host, port),
                                       project=pid),
                        headers=hu.header())

        print('Test Project Recover ok.')


if __name__ == '__main__':
    pytest.main(['test_project.py'])
