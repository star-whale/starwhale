import sys
from e2e import login
from e2e import user
from e2e import project


def get_base_url(host, port):
    return "http://" + host + ":" + port + "/api/v1"


def run(host, port):
    base_url = get_base_url(host, port)
    token = login.test_login(base_url)
    header = {'Authorization': token}
    user.run(base_url, header)
    project.run(base_url, header)


if __name__ == "__main__":
    HOST = "localhost"
    PORT = "8082"
    if len(sys.argv) > 1:
        HOST = sys.argv[1]
    if len(sys.argv) > 2:
        PORT = sys.argv[2]
    run(HOST, PORT)
