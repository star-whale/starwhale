---
title: NMT for french and english
---

This example will illustrate how to evaluate a pre-trained nmt model on StarWhale(`version:0.2.0`) under 7 steps:

* Preparing Data
* Train the model
* Build Model With Starwhale
* Build Runtime With Starwhale
* Build Dataset With Starwhale
* Evaluation process
* Evaluate model on cloud instance

## Prerequisites

* Python3.7 +.
* Starwhale Client.
  * install: `pip install starwhale`
  * check version: `swcli --version`
* Clone starwhale repo

## Preparing Data

* Enter the director `example/nmt`
* Download train data
  * exec `mkdir data && cd data && wget https://www.manythings.org/anki/fra-eng.zip && unzip fra-eng.zip && mv fra.txt eng-fra.txt`

    ```bash
    --2022-06-29 18:29:22--  https://www.manythings.org/anki/fra-eng.zip
      ...
      Saving to: ‘fra-eng.zip’
      ...
      2022-06-29 18:29:26 (2.06 MB/s) - ‘fra-eng.zip’ saved [6612164/6612164]
      Archive:  fra-eng.zip
        inflating: _about.txt
        inflating: fra.txt
    ```

  * Copy the test file we prepared for you

    ```bash
    cp test/test_eng-fra.txt data/test_eng-fra.txt
    ```

## Train NMT Model

* Generate nmt models
  * First, generate vocabulary
    * exec `mkdir models && python3 main.py --mode vocab` and the dir of models/ would generate a file named 'vocab_eng-fra.bin'

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

  * Then, start to train the nmt model
    * exec `python3 main.py --mode train` and finally the dir of models would generate two file which the suffix is .pth(It is recommended to use a machine with gpu)

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

## Build Model With Starwhale

* Develop the evaluation process with Starwhale Python SDK, full code is [here](https://github.com/star-whale/starwhale/blob/main/example/nmt/code/ppl.py).

  ```python
  from starwhale.api.model import PipelineHandler

  class NMTPipeline(PipelineHandler):

      def __init__(self) -> None:
          super().__init__(merge_label=True, ignore_error=True)
          self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
          self.vocab = self._load_vocab()
          self.encoder = self._load_encoder_model(self.device)
          self.decoder = self._load_decoder_model(self.device)

      @torch.no_grad()
      def ppl(self, data, **kw):
          print(f"-----> ppl: {len(data)}")
          src_sentences = data.decode().split('\n')
          print("ppl-src sentexces: %s" % len(src_sentences))
          return evaluate_batch(self.device, self.vocab.input_lang, self.vocab.output_lang, src_sentences, self.encoder, self.decoder)

      def handle_label(self, label, **kw):
          labels = label.decode().split('\n')
          print("src labels: %s" % len(labels))
          return labels

      def cmp(self, _data_loader):
          _result, _label = [], []
          for _data in _data_loader:
              _label.extend(_data[self._label_field])
              (result) = _data[self._ppl_data_field]
              _result.extend(result)
          bleu = BLEU(_result, [_label])
          return {'summary': {'bleu_score': bleu}}

      def _load_vocab(self):
          return torch.load(_ROOT_DIR + '/models/vocab_eng-fra.bin')

      def _load_encoder_model(self, device):
          hidden_size = 256
          model = EncoderRNN(self.vocab.input_lang.n_words, hidden_size, device).to(device)
          param = torch.load(_ROOT_DIR + "/models/encoder.pth", device)
          model.load_state_dict(param)
          return model

      def _load_decoder_model(self, device):
          hidden_size = 256
          model = AttnDecoderRNN(self.vocab.output_lang.n_words, hidden_size, device).to(device)
          param = torch.load(_ROOT_DIR + "/models/decoder.pth", device)
          model.load_state_dict(param)
          return model
  ```

* Define `model.yaml`.

  ```yaml
  version: 1.0
  name: nmt
  config:
    - config/hyperparam.json
  run:
    ppl: code.ppl:NMTPipeline
  desc: nmt by pytorch
  tag:
    - nmt
  ```

* Build Starwhale Model(Please execute this command in the directory where 'model.yaml' is located,the same with runtime and dataset build)
   `swcli model build .`

## Build Runtime With Starwhale

* Write requirements.txt

  ```txt
  numpy
  torch
  nltk
  starwhale
  ```

* Build Starwhale Runtime
  * `swcli runtime create -n nmt -m venv --python 3.8 .`

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

  * follow the prompts to execute the activate environment command
    `source /home/**/vscode_space/starwhale/example/nmt/venv/bin/activate`
  * install by requirements
    `python3 -m pip install -r requirements.txt`
  * `swcli runtime build .`

## Build Dataset With Starwhale

* Use Starwhale python SDK to customize dataset generation rules

  ```python
  from starwhale.api.dataset import BuildExecutor

  class DataSetProcessExecutor(BuildExecutor):
      def iter_data_slice(self, path: str):
          pairs = prepareData(path)
          index = 0
          lines = len(pairs)
          while True:
              last_index = index
              index += 1
              index = min(index, lines - 1 )
              print('data:%s, %s' % (last_index, index))
              data_batch = [src for src, tgt in pairs[last_index:index]]
              join = "\n".join(data_batch)

              print("res-data:%s" % join)
              yield join.encode()
              if index >= lines - 1:
                  break

      def iter_label_slice(self, path: str):
          pairs = prepareData(path)
          index = 0
          lines = len(pairs)
          while True:
              last_index = index
              index += 1
              index = min(index, lines - 1)

              print('label:%s, %s' % (last_index, index))
              data_batch = [tgt for src, tgt in pairs[last_index:index]]
              join = "\n".join(data_batch)
              print("res-label:%s" % join)
              yield join.encode()
              if index >= lines - 1:
                  break
  ```

* Define `dataset.yaml`.

  ```yaml
  name: nmt
  data_dir: data
  data_filter: "test_eng-fra.txt"
  label_filter: "test_eng-fra.txt"
  process: code.dataset:DataSetProcessExecutor
  desc: nmt data and label test dataset
  tag:
  - bin
  attr:
    alignment_size: 4K
    volume_size: 2M
  ```

* Build Starwhale Dataset
  * `swcli dataset build .`

## Evaluation process

* Create evaluate job for the models
  * exec `swcli -vvv job create --model nmt/version/latest --dataset nmt/version/latest --runtime nmt/version/latest`.

      ```bash
      ...
      👏 finish run ppl: PipelineHandler ... result@/home/**/.cache/starwhale/self/job/gq/gqzdgzrzgfstmylbgrstmnjrpi3xeni/ppl/result
      ...
      👏 finish run cmp: PipelineHandler ... result@/home/**/.cache/starwhale/self/job/gq/gqzdgzrzgfstmylbgrstmnjrpi3xeni/cmp/result
        7 out of 7 steps finished ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 0:00:03
      👏 success to create job(project id: local/project/self)
      🐦 run cmd to fetch job info: swcli job info gqzdgzrzgfst
      ```

  * Follow the prompts to execute the info command:`swcli job info gqzdgzrzgfst`

    ```bash
    {
        'created_at': '2022-07-01 10:41:07 CST',
        'datasets': [
            'local/project/self/dataset/nmt/version/latest'
        ],
        'desc': None,
        'finished_at': '2022-07-01 10:41:10 CST',
        'model': 'nmt/version/latest',
        'model_dir': '/home/**/.cache/starwhale/self/workdir/model/nmt/gq/gqygmzddgaywkztdg42gizjvo5utcni/src',
        'name': None,
        'phase': 'all',
        'runtime': 'nmt/version/latest',
        'status': 'success',
        'version': 'gqzdgzrzgfstmylbgrstmnjrpi3xeni'
    }
    ─────────────────────────────────────────── Evaluation process dirs ───────────────────────────────────────────
    🌵 ppl: /home/**/.cache/starwhale/self/job/gq/gqzdgzrzgfstmylbgrstmnjrpi3xeni/ppl
    🐫 cmp: /home/**/.cache/starwhale/self/job/gq/gqzdgzrzgfstmylbgrstmnjrpi3xeni/cmp
    ```

  * the prompts 'cmp: /home/**/.cache/starwhale/self/job/gq/gqzdgzrzgfstmylbgrstmnjrpi3xeni/cmp' show the dir where the result file locate, so we can view the result file content by `cat /home/**/.cache/starwhale/self/job/gq/gqzdgzrzgfstmylbgrstmnjrpi3xeni/cmp/result/current`

      ```bash
      {"summary": {"bleu_score": 0.0}}
      ```

## Evaluate model on cloud instance

* **Login on one cloud instance**

```shell
(nmt) $ swcli instance login http://console.pre.intra.starwhale.ai --username starwhale --password abcd1234 --alias pre-k8s
👨‍🍳 login http://console.pre.intra.starwhale.ai successfully!
```

* **Copy the model to cloud instance**

```shell
(nmt) $ swcli model copy nmt/version/latest cloud://pre-k8s/project/1
🚧 start to copy local/project/self/model/nmt/version/latest -> http://console.pre.intra.starwhale.ai/project/1...
  🎳 upload gqygmzddgaywkztdg42gizjvo5utcni.swmp ━━━━━━━━━━━━━ 100% 0:00:05 71.0 MB 9.1 MB/s
👏 copy done.
```

* **Copy the dataset to cloud instance**

```shell
(nmt) $ swcli dataset copy nmt/version/latest cloud://pre-k8s/project/1
🚧 start to copy local/project/self/dataset/nmt/version/latest -> http://console.pre.intra.starwhale.ai/project/1...
  ⬆ _manifest.yaml         ━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 1.2 kB
  ⬆ data_ubyte_0.swds_bin  ━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 8.1 kB
  ⬆ index.jsonl            ━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 338 bytes
  ⬆ label_ubyte_0.swds_bin ━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 12.2 kB
  ⬆ archive.swds_meta      ━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 81.9 kB
👏 copy done
```

* **Copy the runtime to cloud instance**

```shell
(nmt) $ swcli runtime copy nmt/version/latest cloud://pre-k8s/project/1
🚧 start to copy local/project/self/runtime/nmt/version/latest -> http://console.pre.intra.starwhale.ai/project/1...
  🎳 upload mjsdkzbymmztmztdg42gizjvge2honq.swrt ━━━━━━━━━━━━━━━━━━━ 100% 0:00:00 20.5 kB
👏 copy done.
```

* **Go to the console and create one job**
