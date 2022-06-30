---
title: NMT for french and engish
---
## Prerequisites

* Python3.7 +.
* starwhale. 
  * install: `pip install starwhale`
  * valid: `swcli --version`
* Clone starwhale repo

- preapring
  - exec `cd example/nmt`
  - download data
    - exec `mkdir data && cd data && wget https://www.manythings.org/anki/fra-eng.zip && unzip fra-eng.zip && mv fra.txt eng-fra.txt`
      ```bash
      --2022-06-29 18:29:22--  https://www.manythings.org/anki/fra-eng.zip
        Resolving www.manythings.org (www.manythings.org)... 172.67.186.54, 104.21.92.44, 2606:4700:3030::6815:5c2c, ...
        Connecting to www.manythings.org (www.manythings.org)|172.67.186.54|:443... connected.
        HTTP request sent, awaiting response... 200 OK
        Length: 6612164 (6.3M) [application/zip]
        Saving to: ‘fra-eng.zip’

        fra-eng.zip 100%[=====================================================================>] 6.31M 2.06MB/s in 3.1s    

        2022-06-29 18:29:26 (2.06 MB/s) - ‘fra-eng.zip’ saved [6612164/6612164]

        Archive:  fra-eng.zip
          inflating: _about.txt              
          inflating: fra.txt 
      ```
    - Select a part of the file eng-fra.txt as the new test file(named 'test_eng-fra.txt') data.Simply put, 100 items can be randomly selected
      ```bash
      cp test/test_eng-fra.txt data/test_eng-fra.txt  
      ```
  - generate nmt models
    - first, generate vocabulary
      - exec `mkdir models && python3 main.py --mode vocab` and the dir of models/ would generate a file named 'vocab_eng-fra.bin'
         ```bash
          Reading lines...
          Read 194513 sentence pairs
          Trimmed to 194513 sentence pairs
          Counting words...
          Counted words:
          eng 15140
          fra 24329
          generated vocabulary, source 15140 words, target 24329 words
          vocabulary saved to models/vocab_eng-fra.bin
         ```
    - then, start to train the nmt model
      - exec `python3 main.py --mode train` and finally the dir of models would generate two file which the suffix is .pth(It is recommended to use a machine with gpu)
        ```bash
        preapring data...
        start to train...
        0m 22s (- 3m 21s) (1000 10%) 5.2330
        0m 44s (- 2m 57s) (2000 20%) 4.8755
        1m 6s (- 2m 35s) (3000 30%) 4.6942
        1m 29s (- 2m 13s) (4000 40%) 4.5593
        1m 49s (- 1m 49s) (5000 50%) 4.5472
        2m 12s (- 1m 28s) (6000 60%) 4.4088
        2m 36s (- 1m 6s) (7000 70%) 4.3386
        2m 58s (- 0m 44s) (8000 80%) 4.3486
        3m 23s (- 0m 22s) (9000 90%) 4.2323
        3m 46s (- 0m 0s) (10000 100%) 4.1829
        Saving model to /home/**/starwhale/example/nmt/models/encoder.pth
        Saving model to /home/**/starwhale/example/nmt/models/decoder.pth
        ```
- use starwhale cli to evaluate the models
  - build swmp(sw model package)\swrt(sw runtime)\swds(sw dataset) 
    - build starwhale model
      - `swcli model build .`
    - build starwhale runtime
      - `swcli runtime create -n nmt -m venv --python 3.8 .`
        ```bash
        2022-06-30 20:09:14.247 | DEBUG  | verbosity: 4, log level: DEBUG
        🚧 start to create runtime environment...
        2022-06-30 20:09:14.250 | INFO   | create venv @ /home/**/vscode_space/starwhale/example/nmt/venv...
        👏 create venv@/home/**/vscode_space/starwhale/example/nmt/venv, python:3.8.10 (default, Mar 15 2022, 12:22:08)
        [GCC 9.4.0]
        🐶 install starwhale==0.2.0b7 venv@/home/**/vscode_space/starwhale/example/nmt/venv...
        🍰 run command in shell 🍰
        source /home/**/vscode_space/starwhale/example/nmt/venv/bin/activate
        👏 python runtime environment is ready to use 🎉
        ```
      - follow the prompts to execute the activate environment command
        `source /home/**/vscode_space/starwhale/example/nmt/venv/bin/activate`
      - install by requirements
        `python3 -m pip install -r requirements.txt`
      - `swcli runtime build .`
    - build starwhale dataset
      - `swcli dataset build .`
  - create evaluate job for the models
    - exec `swcli -vvv job create --model nmt/version/latest --dataset nmt/version/latest --runtime nmt/version/latest`.