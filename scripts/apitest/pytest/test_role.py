import requests
import pytest
import time
import os
from common import httputil as hu


def get_current_user_role(host, port, project, header):
    url = '{api}/user/current/role'
    res = requests.get(url=url.format(api=hu.url(host, port)),
                       params='projectUrl=' + project,
                       headers=header)
    return res


class TestRole:
    def test_create_user(self, host, port):
        hu.create_tmp_user(host, port)

    def test_current_role(self, host, port):
        url = '{api}/user/current/role'
        res = requests.get(url=url.format(api=hu.url(host, port)),
                           params='projectUrl=0',
                           headers=hu.tmp_user_header())
        response = res.json()

        assert res.status_code == 200
        assert len(response['data']) > 0
        assert response['data'][0]['role']['code'] == 'MAINTAINER'

    def test_add_project_role(self, host, port):
        sec = str(int(time.time()))
        project_name = 'pytest' + sec
        url = '{api}/project'
        res = requests.post(url=url.format(api=hu.url(host, port)),
                            json={'projectName': project_name,
                                  'privacy': 'private',
                                  'ownerId': 1,
                                  'description': 'project for role pytest'},
                            headers=hu.header())
        assert res.status_code == 200
        os.environ['project_name'] = project_name

        url = '{api}/project/{project}/role'
        res = requests.post(url=url.format(api=hu.url(host, port),
                                           project=project_name),
                            params='roleId=1&userId=' + hu.tmp_user_id(),
                            headers=hu.header())
        assert res.status_code == 200

        project_name = os.getenv('project_name')
        res = get_current_user_role(host, port, project_name,
                                    hu.tmp_user_header())
        response = res.json()

        assert res.status_code == 200
        assert len(response['data']) > 0
        assert response['data'][0]['role']['code'] == 'OWNER'
        role_id = response['data'][0]['id']
        os.environ['role_id'] = role_id

    def test_modify_project_role(self, host, port):
        project_name = os.getenv('project_name')
        role_id = os.getenv('role_id')

        url = '{api}/project/{project}/role/{role}'
        res = requests.put(url=url.format(api=hu.url(host, port),
                                          project=project_name,
                                          role=role_id),
                           params='roleId=2',
                           headers=hu.header())
        assert res.status_code == 200

        res = get_current_user_role(host, port, project_name,
                                    hu.tmp_user_header())
        response = res.json()

        assert res.status_code == 200
        assert len(response['data']) > 0
        assert response['data'][0]['role']['code'] == 'MAINTAINER'

    def test_delete_project_role(self, host, port):
        project_name = os.getenv('project_name')
        role_id = os.getenv('role_id')
        url = '{api}/project/{project}/role/{role}'
        res = requests.delete(url=url.format(api=hu.url(host, port),
                                             project=project_name,
                                             role=role_id),
                              headers=hu.header())
        assert res.status_code == 200

    def test_destroy(self, host, port):
        url = '{api}/project/{project}'
        project_name = os.getenv('project_name')
        res = requests.delete(url=url.format(api=hu.url(host, port),
                                             project=project_name),
                              headers=hu.header())
        assert res.status_code == 200

        hu.tmp_user_destroy(host, port)


if __name__ == '__main__':
    pytest.main(['test_role.py'])
