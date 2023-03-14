---
title: 可以作为K8s节点的设备
---

## 特征

此类设备可以作为一个独立的 K8s 节点运行，节点的概念可以参考 K8s [官方文档](https://kubernetes.io/docs/concepts/architecture/nodes/)
简单来讲，设备至少支持安装和运行

* [kubelet](https://kubernetes.io/docs/reference/generated/kubelet)
* [kube-proxy](https://kubernetes.io/docs/reference/command-line-tools-reference/kube-proxy/)
* [container runtime](https://kubernetes.io/docs/setup/production-environment/container-runtimes)

这类设备接入之后，通常需要定制 runtime 镜像来运行 Starwhale 的 [Job](/zh/docs/overview/concepts#7-job-step-and-task)

## 典型设备

### 树莓派

[树莓派](https://www.raspberrypi.com/) 是我们熟知的微型电脑, 除了是 ARM 架构外, 使用起来和常规的服务器区别不大, 下面简单介绍树莓派加入 K8s 集群的步骤.

1. 安装操作系统, 建议使用基于 Ubuntu 的 [Raspberry Pi OS](https://www.raspberrypi.com/software/)
2. 安装 Docker 环境, 按照 Docker [官方文档](https://docs.docker.com/engine/install/debian/) 的说明操作即可完成安装
3. 加入已有的 K8s 集群, 过程中如果遇到问题, 可以参考 [Kubernetes The Hard Way](https://github.com/kelseyhightower/kubernetes-the-hard-way)

成功加入集群之后, 就可以参考后续的文档将任务调度到树莓派上进行实验了.

### Jetson

[Jetson](https://www.nvidia.com/en-us/autonomous-machines/embedded-systems/) 是 Intel 公司出品的一类高性能嵌入式设备，内置 GPU
我们使用它通常是要充分利用它的GPU计算资源

后面我们以 orin 为例进行环境配置的说明

#### 节点环境初始化

按照官方的文档初始化好定制的 Ubuntu 系统以及 JetPack 套件, 保证能够成功运行官方提供的demo, 拥有 docker 环境. [orin 的初始化文档](https://developer.nvidia.com/embedded/learn/get-started-jetson-agx-orin-devkit)

#### 配置 GPU

K8s 基于 [device plugin](https://kubernetes.io/docs/concepts/extend-kubernetes/compute-storage-net/device-plugins/) 机制支持将硬件资源发布到集群.
NVIDIA 为它的 GPU 提供了自己的 [k8s device plugin](https://github.com/NVIDIA/k8s-device-plugin), 并且从 [v0.13.0-rc.1](https://github.com/NVIDIA/k8s-device-plugin/releases/tag/v0.13.0-rc.1) 开始支持了 Jetson 系列设备

1. 配置 nvidia-container-runtime 作为默认的 runtime, 参考[链接](https://github.com/NVIDIA/k8s-device-plugin#preparing-your-gpu-nodes)
    比如如果使用的是docker, 则需要配置 `/etc/docker/daemon.json` 包含下面内容

    ```json
    {
        "runtimes": {
            "nvidia": {
                "path": "nvidia-container-runtime",
                "runtimeArgs": []
            }
        },

        "default-runtime": "nvidia"
    }
    ```

2. 使用 Jetson 官方教程中提到的 `deviceQuery` 测试在 docker 中使用 GPU

    如果有类似下面的输出, 则我们的 docker 环境已经配置好

    ```sh
    # docker run --rm -v `/path/to/deviceQuery`:/root/deviceQuery nvcr.io/nvidia/l4t-jetpack:r35.1.0 /root/deivceQuery

    /root/deviceQuery Starting...

    CUDA Device Query (Runtime API) version (CUDART static linking)

    Detected 1 CUDA Capable device(s)

    Device 0: "Orin"
    CUDA Driver Version / Runtime Version          11.4 / 11.4
    CUDA Capability Major/Minor version number:    8.7
    Total amount of global memory:                 30623 MBytes (32110190592 bytes)
    (016) Multiprocessors, (128) CUDA Cores/MP:    2048 CUDA Cores
    GPU Max Clock rate:                            1300 MHz (1.30 GHz)
    Memory Clock rate:                             1300 Mhz
    Memory Bus Width:                              128-bit
    L2 Cache Size:                                 4194304 bytes
    Maximum Texture Dimension Size (x,y,z)         1D=(131072), 2D=(131072, 65536), 3D=(16384, 16384, 16384)
    Maximum Layered 1D Texture Size, (num) layers  1D=(32768), 2048 layers
    Maximum Layered 2D Texture Size, (num) layers  2D=(32768, 32768), 2048 layers
    Total amount of constant memory:               65536 bytes
    Total amount of shared memory per block:       49152 bytes
    Total shared memory per multiprocessor:        167936 bytes
    Total number of registers available per block: 65536
    Warp size:                                     32
    Maximum number of threads per multiprocessor:  1536
    Maximum number of threads per block:           1024
    Max dimension size of a thread block (x,y,z): (1024, 1024, 64)
    Max dimension size of a grid size    (x,y,z): (2147483647, 65535, 65535)
    Maximum memory pitch:                          2147483647 bytes
    Texture alignment:                             512 bytes
    Concurrent copy and kernel execution:          Yes with 2 copy engine(s)
    Run time limit on kernels:                     No
    Integrated GPU sharing Host Memory:            Yes
    Support host page-locked memory mapping:       Yes
    Alignment requirement for Surfaces:            Yes
    Device has ECC support:                        Disabled
    Device supports Unified Addressing (UVA):      Yes
    Device supports Managed Memory:                Yes
    Device supports Compute Preemption:            Yes
    Supports Cooperative Kernel Launch:            Yes
    Supports MultiDevice Co-op Kernel Launch:      Yes
    Device PCI Domain ID / Bus ID / location ID:   0 / 0 / 0
    Compute Mode:
        < Default (multiple host threads can use ::cudaSetDevice() with device simultaneously) >

    deviceQuery, CUDA Driver = CUDART, CUDA Driver Version = 11.4, CUDA Runtime Version = 11.4, NumDevs = 1
    ```

3. 将节点加入 K8s 集群
    此环节和服务器无差别, 细节参考 K8s 相关文档完成即可

4. 配置 device plugin 的 daemon set
    参考 [链接](https://github.com/NVIDIA/k8s-device-plugin#enabling-gpu-support-in-kubernetes)

    以 `v0.13.0-rc.1` 为例

    ```sh
    kubectl create -f https://raw.githubusercontent.com/NVIDIA/k8s-device-plugin/v0.13.0-rc.1/nvidia-device-plugin.yml
    ```

    注意: 此操作会在所有的 K8s 节点中运行 NVIDIA 的 device plugin 插件, 如果之前配置过, 则会被更新, 请谨慎评估使用的镜像版本

5. 确认 GPU 可以在集群中发现和使用
    参考下边命令, 查看 Jetson 节点的 Capacity 中有 `nvidia.com/gpu`, GPU 即被 K8s 集群正常识别

    ```sh
    # kubectl describe node orin | grep -A15 Capacity
    Capacity:
    cpu:                12
    ephemeral-storage:  59549612Ki
    hugepages-1Gi:      0
    hugepages-2Mi:      0
    hugepages-32Mi:     0
    hugepages-64Ki:     0
    memory:             31357608Ki
    nvidia.com/gpu:     1
    pods:               110
    ```

#### 制作和使用自定义镜像

文章前面提到的 l4t-jetpack 镜像可以满足我们一般的使用, 如果我们需要自己定制更加精简或者更多功能的镜像, 可以基于 l4t-base 来制作
相关 Dockerfile 可以参考 [Starwhale为mnist制作的镜像](https://github.com/star-whale/starwhale/tree/main/docker/devices/jetson)
