FROM ubuntu:20.04
WORKDIR /root
RUN apt update && apt install -y python3-pip && pip3 install requests fastapi uvicorn
RUN mkdir broker && touch broker/__init__.py
COPY broker.py broker/broker.py
COPY common.py broker/common.py
CMD ["uvicorn", "broker.broker:app", "--host=0.0.0.0"]
