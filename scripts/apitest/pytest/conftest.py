import pytest


def pytest_addoption(parser):
    parser.addoption(
        "--host", action="store", default="localhost", help="host"
    )
    parser.addoption(
        "--port", action="store", default="8082", help="port"
    )


@pytest.fixture
def host(request):
    return request.config.getoption("--host")


@pytest.fixture
def port(request):
    return request.config.getoption("--port")

