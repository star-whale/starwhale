#!/bin/bash -x

cd $1/apitest/pytest
pip install -r requirements.txt
pytest --host $2 --port $3