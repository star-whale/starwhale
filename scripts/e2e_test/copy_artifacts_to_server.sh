#!/bin/bash -x

source venv/bin/activate
swcli instance login http://$1 --username starwhale --password abcd1234 --alias pre-k8s
swcli model copy mnist/version/latest cloud://pre-k8s/project/1
swcli dataset copy mnist/version/latest cloud://pre-k8s/project/1
swcli runtime copy pytorch-mnist/version/latest cloud://pre-k8s/project/1
