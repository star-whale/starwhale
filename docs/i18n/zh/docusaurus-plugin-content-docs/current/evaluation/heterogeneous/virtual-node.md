---
title: 基于 Virtual Kubelet 管理无法作为K8s节点的设备
---

## 简介

[Virtual Kubelet](https://virtual-kubelet.io/) 是一个开源的框架, 可以通过模拟 kubelet 和 K8s 集群通信的方式伪装成一个 K8s 节点.
此方案被各云厂商广泛用于 serverless 容器集群方案, 比如阿里云的 ASK, Amazon 的 AWS Fargate 等.

## 原理

virtual kubelet 框架将 kubelet 对于 Node 的相关接口进行实现, 只需要简单的配置即可模拟一个节点.
我们只需要实现 [PodLifecycleHandler](https://github.com/virtual-kubelet/virtual-kubelet/blob/704b01eac6bdf0472b9c93173e4cb64bd6e53e94/node/podcontroller.go#L47) 接口即可支持:

* 创建, 更新, 删除 Pod
* 获取 Pod 状态
* 获取 Container 日志

## 将设备加入集群

如果我们的设备由于资源限制等情况无法作为 K8s 的一个节点进行服务, 那么我们可以通过使用 virtual kubelet 模拟一个代理节点的方式对这些设备进行管理,
Starwhale Controller 和设备的控制流如下

```text

 ┌──────────────────────┐      ┌────────────────┐     ┌─────────────────┐     ┌────────────┐
 │ Starwhale Controller ├─────►│ K8s API Server ├────►│ virtual kubelet ├────►│ Our device │
 └──────────────────────┘      └────────────────┘     └─────────────────┘     └────────────┘

```

virtual kubelet 将 Starwhale Controller 下发下来的 Pod 编排信息转化为对设备的控制行为, 比如 ssh 到设备上执行一段命令, 或者通过 USB 或者串口发送一段消息等.  

下面是使用 virtual kubelet 的方式来对一个未加入集群的可以 ssh 的设备进行控制的示例

1. 准备证书

* 创建文件 vklet.csr, 内容如下

```ini
[req]
req_extensions = v3_req
distinguished_name = req_distinguished_name
[req_distinguished_name]
[v3_req]
basicConstraints = CA:FALSE
keyUsage = digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names
[alt_names]
IP = 1.2.3.4
```

* 生成证书

```sh
openssl genrsa -out vklet-key.pem 2048
openssl req -new -key vklet-key.pem -out vklet.csr -subj '/CN=system:node:1.2.3.4;/C=US/O=system:nodes' -config ./csr.conf 
```

* 提交证书

```sh
cat vklet.csr| base64 | tr -d "\n" # 输出内容作为 csr.yaml 文件中 spec.request 的内容
```

csr.yaml

```yaml
apiVersion: certificates.k8s.io/v1
kind: CertificateSigningRequest
metadata:
  name: vklet
spec:
  request: ******************************************************
  signerName: kubernetes.io/kube-apiserver-client
  expirationSeconds: 1086400
  usages:
  - client auth
```

```sh
 kubectl apply -f csr.yaml
 kubectl certificate approve vklet
 kubectl get csr vklet -o jsonpath='{.status.certificate}'| base64 -d > vklet-cert.pem
```

现在我们得到了 vklet-cert.pem

* 编译 virtual kubelet

```sh
git clone https://github.com/virtual-kubelet/virtual-kubelet
cd virtual-kubelet && make build
```

创建节点的配置文件 mock.json

```json
{
    "virtual-kubelet":
    {
        "cpu": "100",
        "memory": "100Gi",
        "pods": "100"
    }
}
```

启动 virtual kubelet

```sh
export APISERVER_CERT_LOCATION=/path/to/vklet-cert.pem
export APISERVER_KEY_LOCATION=/path/to/vklet-key.pem
export KUBECONFIG=/path/to/kubeconfig

virtual-kubelet --provider mock --provider-config /path/to/mock.json
```

至此, 我们使用 virtual kubelet 模拟了一个 100 core + 100G 内存的节点.

* 增加 `PodLifecycleHandler` 的实现, 将 Pod 编排中的重要信息转化为 ssh 命令执行, 并且收集日志待 Starwhale Controller 收集

具体实现可参考 [ssh executor](https://github.com/jialeicui/remote-provider/tree/master/cmd/virtual-kubelet/internal/provider/mock)
