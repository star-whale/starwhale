import requests
import pytest
from common import httputil as hu


class TestEvaluation:

    def test_login(self, host, port):
        hu.login(host, port)

    def test_attributes(self, host, port):
        res = requests.get(url=hu.url(host, port) + '/project'
                                                    '/1'
                                                    '/evaluation'
                                                    '/view/attribute',
                           headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response['code'] == 'success'
        assert len(response['data']) > 0

        print('Test Evaluation Attributes list ok.')

    def test_config_create(self, host, port):
        data = {'name': 'test1', 'content': '{"text": "config content"}'}
        res = requests.post(url=hu.url(host, port) + '/project'
                                                     '/1'
                                                     '/evaluation'
                                                     '/view/config',
                            json=data,
                            headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response['code'] == 'success'

        print('Test Evaluation create config ok.')

    def test_config_get(self, host, port):
        res = requests.get(url=hu.url(host, port) + '/project'
                                                    '/1'
                                                    '/evaluation'
                                                    '/view/config',
                           params='name=test1',
                           headers=hu.header())
        response = res.json()

        assert res.status_code == 200
        assert response['code'] == 'success'
        assert response['data']['name'] == 'test1'

        print('Test Evaluation get config ok.')

    def test_summary_list(self, host, port):
        res = requests.get(url=hu.url(host, port) + '/project'
                                                    '/1'
                                                    '/evaluation',
                           params='filter=',
                           headers=hu.header())
        response = res.json()
        assert res.status_code == 200
        assert response['code'] == 'success'

        print('Test Evaluation summary list ok.')


if __name__ == '__main__':
    pytest.main(['test_evaluation.py'])
