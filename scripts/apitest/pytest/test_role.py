import requests
import pytest
import time
import os
from common import httputil as hu


def header():
    return {'Authorization': os.getenv('member_token')}


class TestRole:
    def test_create_user(self, host, port):
        hu.login(host, port)

        url = '{api}/user'
        sec = str(int(time.time()))
        user_name = 'pt_user' + sec
        user_password = 'abcd'
        os.environ['user_name'] = user_name
        res = requests.post(url=url.format(api=hu.url(host, port)),
                            json={'userName': user_name,
                                  'userPwd': user_password},
                            headers=hu.header())
        response = res.json()
        assert res.status_code == 200
        uid = response['data']
        os.environ['user_id'] = uid

        url = '{api}/login'
        res = requests.post(url=url.format(api=hu.url(host, port)),
                            data={'userName': user_name,
                                  'userPwd': user_password})
        assert res.status_code == 200
        token = res.headers['Authorization']
        os.environ['member_token'] = token

    def test_current_role(self, host, port):
        url = '{api}/user/current/role'
        res = requests.get(url=url.format(api=hu.url(host, port)),
                           params='projectUrl=0',
                           headers=header())
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
                            params='roleId=1&userId=' + os.getenv('user_id'),
                            headers=hu.header())
        assert res.status_code == 200

        url = '{api}/user/current/role'
        project_name = os.getenv('project_name')
        res = requests.get(url=url.format(api=hu.url(host, port)),
                           params='projectUrl=' + project_name,
                           headers=header())
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

        url = '{api}/user/current/role'
        res = requests.get(url=url.format(api=hu.url(host, port)),
                           params='projectUrl=' + project_name,
                           headers=header())
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

        url = '{api}/user/{user}/state'
        user_id = os.getenv('user_id')
        res = requests.put(url=url.format(api=hu.url(host, port),
                                          user=user_id),
                           json={'isEnabled': 'false'},
                           headers=hu.header())
        assert res.status_code == 200


if __name__ == '__main__':
    pytest.main(['test_role.py'])
