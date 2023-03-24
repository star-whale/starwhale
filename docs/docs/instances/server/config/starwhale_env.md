---
title: starwhale.env
---

```text
################################################################################
# *** Required ***
# The external Starwhale server URL. For example: https://cloud.starwhale.ai
SW_INSTANCE_URI=

# The listening port of Starwhale Server
SW_CONTROLLER_PORT=8082

# The maximum upload file size. This setting affects datasets and models uploading when copied from outside.
SW_UPLOAD_MAX_FILE_SIZE=20480MB
################################################################################
# The base URL of the Python Package Index to use when creating a runtime environment.
SW_PYPI_INDEX_URL=http://10.131.0.1/repository/pypi-hosted/simple/ 

# Extra URLs of package indexes to use in addition to the base url.
SW_PYPI_EXTRA_INDEX_URL=

# Space separated hostnames. When any host specified in the base URL or extra URLs does not have a valid SSL 
# certification, use this option to trust it anyway.
SW_PYPI_TRUSTED_HOST=
################################################################################
# The JWT token expiration time. When the token expires, the server will request the user to login again.
SW_JWT_TOKEN_EXPIRE_MINUTES=43200

# *** Required ***
# The JWT secret key. All strings are valid, but we strongly recommend you to use a random string with at least 16 characters.
SW_JWT_SECRET=
################################################################################
# The Kubernetes namspace to use when running a task
SW_K8S_NAME_SPACE=default

# The path on the Kubernetes host node's filesystem to cache Python packages. Use the setting only if you have
# the permission to use host node's filesystem. The runtime environment setup process may be accelerated when the host
# path cache is used. Leave it blank if you do not want to use it.
SW_K8S_HOST_PATH_FOR_CACHE=

###############################################################################
# *** Required ***
# The object storage system type. Valid values are:
# s3: [AWS S3](https://aws.amazon.com/s3) or other s3-compatible object storage systems
# aliyun: [Aliyun OSS](https://www.alibabacloud.com/product/object-storage-service)
# minio: [MinIO](https://min.io)
# file: Local filesystem
SW_STORAGE_TYPE=

# The path prefix for all data saved on the storage system.
SW_STORAGE_PREFIX=
################################################################################
# The following settings are only used when SW_STORAGE_TYPE is file.

# The root directory to save data.
# This setting is only used when SW_STORAGE_TYPE is file. 
SW_STORAGE_FS_ROOT_DIR=/usr/local/starwhale
################################################################################
# The following settings are only used when SW_STORAGE_TYPE is not file.

# *** Required ***
# The name of the bucket to save data.
SW_STORAGE_BUCKET=

# *** Required ***
# The endpoint URL of the object storage service. 
# This setting is only used when SW_STORAGE_TYPE is s3 or aliyun.
SW_STORAGE_ENDPOINT=

# *** Required ***
# The access key used to access the object storage system.
SW_STORAGE_ACCESSKEY=

# *** Required ***
# The secret access key used to access the object storage system.
SW_STORAGE_SECRETKEY=

# Starwhale Server will use multipart upload when uploading a large file. This setting specifies the part size.
SW_STORAGE_PART_SIZE=5MB
################################################################################
# MySQL settings

# *** Required ***
# The hostname/IP of the MySQL server.
SW_METADATA_STORAGE_IP=

# The port of the MySQL server.
SW_METADATA_STORAGE_PORT=3306

# *** Required ***
# The database used by Starwhale Server
SW_METADATA_STORAGE_DB=starwhale

# *** Required ***
# The username of the MySQL server.
SW_METADATA_STORAGE_USER=

# *** Required ***
# The password of the MySQL server.
SW_METADATA_STORAGE_PASSWORD=
################################################################################
```
