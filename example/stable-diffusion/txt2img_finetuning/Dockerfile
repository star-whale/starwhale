FROM ghcr.io/star-whale/starwhale:0.4.0

ARG PY_VER=3.10

ADD requirements.txt .
ADD train_text_to_image.py .

RUN update-alternatives --install /usr/bin/python3 python3 /usr/bin/python${PY_VER} 10 \
	&& python3 -m pip install -i https://mirrors.ustc.edu.cn/pypi/web/simple -r requirements.txt \
	&& python3 -m pip cache purge \
	&& rm requirements.txt

#ADD entrypoint /usr/local/bin/entrypoint

WORKDIR .
CMD ["python3", "train_text_to_image.py"]

