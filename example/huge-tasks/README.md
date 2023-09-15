# Huge Tasks Demo

Show Starwhale replicas feature.

## Building Guides

### Installing Starwhale

```bash
python3 -m pip install starwhale==0.5.12
```

### Building Starwhale Runtime

```bash
swcli runtime build --yaml runtime.yaml --name huge-tasks
```

### Building Starwhale Dataset

```bash
swcli runtime activate huge-tasks
python3 dataset.py
# swcli dataset build
```

### Building Starwhale Model

```bash
swcli model build . -m evaluation --runtime huge-tasks --name huge-tasks
```

## Run Guides

### Run in Starwhale Standalone

Run model evaluation:

```bash
swcli -vv model run --dataset huge-tasks-random-text --uri huge-tasks
```
