ARG BASE_IMAGE=starwhaleai/base:latest
FROM ${BASE_IMAGE}

ARG SW_VERSION=0.1.0
ENV SW_VERSION ${SW_VERSION}

RUN pip install starwhale==${SW_VERSION}