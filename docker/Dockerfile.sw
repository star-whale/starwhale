ARG BASE_IMAGE=starwhaleai/base:latest
FROM ${BASE_IMAGE}

ARG SW_VERSION=0.1.0
ENV SW_VERSION ${SW_VERSION}

RUN rm -rf /opt/starwhale.venv \
    && virtualenv /opt/starwhale.venv \
    && /opt/starwhale.venv/bin/python -m pip install starwhale==${SW_VERSION} \
    && rm -rf /usr/local/bin/swcli /usr/local/bin/sw-docker-entrypoint \
    && ln -s /opt/starwhale.venv/bin/swcli /usr/local/bin/swcli \
    && ln -s /opt/starwhale.venv/bin/sw-docker-entrypoint /usr/local/bin/sw-docker-entrypoint

#TODO: add conda channel switch
COPY external/condarc /root/.condarc
WORKDIR /opt/starwhale/swmp

ENV SW_SWMP_WORKDIR=/opt/starwhale/swmp
ENV SW_TASK_INPUT_CONFIG=/opt/starwhale/config/input.json
ENV SW_TASK_STATUS_DIR=/opt/starwhale/status
ENV SW_TASK_LOG_DIR=/opt/starwhale/log
ENV SW_TASK_RESULT_DIR=/opt/starwhale/result

ENTRYPOINT ["/usr/local/bin/sw-docker-entrypoint"]