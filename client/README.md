# StarWhale Client and Python SDK

# Develop:
- conda create a development env
- cd client dir
- pip install -e
- run "swcli --help" or "python -m starwhale --help"
- docker run -it -v ~/.cache/minio:/data -p 9000:9000 -p 9001:9001 quay.io/minio/minio:latest server /data --console-address ":9001"