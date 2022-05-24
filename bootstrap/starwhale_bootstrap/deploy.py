import os

import ansible_runner


def deploy(
    logdir: str = "", extravars: dict = {}, cmdline: str = "", inventory: dict = {}
):
    cur_path = os.path.abspath(os.path.dirname(__file__))
    if not os.path.exists(logdir):
        os.makedirs(logdir)
    # for issue https://github.com/ansible/ansible-runner/issues/853
    hosts_path = logdir + "/inventory/hosts.json"
    if os.path.exists(hosts_path):
        os.remove(hosts_path)

    r = ansible_runner.run(
        private_data_dir=logdir,
        playbook=cur_path + "/playbook/bootstrap.yaml",
        roles_path=cur_path + "/playbook/roles",
        extravars=extravars,
        cmdline=cmdline,
        inventory=inventory
        # optional
        # ssh_key='',
    )
    print("status:{}".format(r.status))
    # successful: 0
    print("Final result:")
    print(r.stats)
