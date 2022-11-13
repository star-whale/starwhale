import json
import logging
from collections import OrderedDict

import numpy as np
import torch
import torch.nn as nn


def xavier(net):
    def weights_init(m):
        classname = m.__class__.__name__
        if classname.find("Conv") != -1 and hasattr(m, "weight"):
            nn.init.xavier_uniform_(m.weight.data, gain=1.0)
            if m.bias is not None:
                m.bias.data.zero_()
        elif classname.find("BatchNorm") != -1:
            m.weight.data.fill_(1.0)
            if m.bias is not None:
                m.bias.data.zero_()
        elif classname.find("Linear") != -1:
            nn.init.xavier_uniform_(m.weight.data, gain=1.0)
            if m.bias is not None:
                m.bias.data.zero_()
        elif (
            classname
            in [
                "Sequential",
                "AvgPool3d",
                "MaxPool3d",
                "Dropout",
                "ReLU",
                "Softmax",
                "BnActConv3d",
            ]
            or "Block" in classname
        ):
            pass
        else:
            if classname != classname.upper():
                logging.warning(f"Initializer:: '{classname}' is uninitialized.")

    net.apply(weights_init)


def init_3d_from_2d_dict(net, state_dict, method="inflation"):
    logging.debug(
        f"Initializer:: loading from 2D neural network, filling method: `{method}' ..."
    )

    # filling method
    def filling_kernel(src, dshape, method):
        assert method in [
            "inflation",
            "random",
        ], f"filling method: {method} is unknown!"

        if method == "inflation":
            dst = torch.FloatTensor(dshape)
            # normalize
            src = src / float(dshape[2])
            src = src.view(dshape[0], dshape[1], 1, dshape[3], dshape[4])
            dst.copy_(src)
        elif method == "random":
            dst = torch.FloatTensor(dshape)
            tmp = torch.FloatTensor(src.shape)
            # normalize
            src = src / float(dshape[2])
            # random range
            scale = src.abs().mean()
            # filling
            dst[:, :, 0, :, :].copy_(src)
            i = 1
            while i < dshape[2]:
                if i + 2 < dshape[2]:
                    nn.init.uniform(tmp, a=-scale, b=scale)
                    dst[:, :, i, :, :].copy_(tmp)
                    dst[:, :, i + 1, :, :].copy_(src)
                    dst[:, :, i + 2, :, :].copy_(-tmp)
                    i += 3
                elif i + 1 < dshape[2]:
                    nn.init.uniform(tmp, a=-scale, b=scale)
                    dst[:, :, i, :, :].copy_(tmp)
                    dst[:, :, i + 1, :, :].copy_(-tmp)
                    i += 2
                else:
                    dst[:, :, i, :, :].copy_(src)
                    i += 1
            # shuffle
            tmp = dst.numpy().swapaxes(2, -1)
            shp = tmp.shape[:-1]
            for ndx in np.ndindex(shp):
                np.random.shuffle(tmp[ndx])
            dst = torch.from_numpy(tmp)
        else:
            raise NotImplementedError

        return dst

    # customized partialy loading function
    src_state_keys = list(state_dict.keys())
    dst_state_keys = list(net.state_dict().keys())
    for name, param in state_dict.items():
        if name in dst_state_keys:
            src_param_shape = param.shape
            dst_param_shape = net.state_dict()[name].shape
            if src_param_shape != dst_param_shape:
                if name.startswith("classifier"):
                    continue
                assert (
                    len(src_param_shape) == 4 and len(dst_param_shape) == 5
                ), f"{name} mismatch"
                if list(src_param_shape) == [dst_param_shape[i] for i in [0, 1, 3, 4]]:
                    if dst_param_shape[2] != 1:
                        param = filling_kernel(
                            src=param, dshape=dst_param_shape, method=method
                        )
                    else:
                        param = param.view(dst_param_shape)
                assert (
                    dst_param_shape == param.shape
                ), f"Initializer:: error({name}): {dst_param_shape} != {param.shape}"
            net.state_dict()[name].copy_(param)
            src_state_keys.remove(name)
            dst_state_keys.remove(name)

    # indicate missing / ignored keys
    if src_state_keys:
        out = "['" + "', '".join(src_state_keys) + "']"
        logging.info(
            f"Initializer:: >> {len(src_state_keys)} params are "
            f"unused: {out if len(out) < 300 else out[0:150] + ' ... ' + out[-150:]}"
        )
    if dst_state_keys:
        logging.info(
            f"Initializer:: >> failed to load: \n{json.dumps(dst_state_keys, indent=4, sort_keys=True)}"
        )


class BN_AC_CONV3D(nn.Module):
    def __init__(
        self,
        num_in,
        num_filter,
        kernel=(1, 1, 1),
        pad=(0, 0, 0),
        stride=(1, 1, 1),
        g=1,
        bias=False,
    ):
        super(BN_AC_CONV3D, self).__init__()
        self.bn = nn.BatchNorm3d(num_in)
        self.relu = nn.ReLU(inplace=True)
        self.conv = nn.Conv3d(
            num_in,
            num_filter,
            kernel_size=kernel,
            padding=pad,
            stride=stride,
            groups=g,
            bias=bias,
        )

    def forward(self, x):
        h = self.relu(self.bn(x))
        h = self.conv(h)
        return h


class MF_UNIT(nn.Module):
    def __init__(
        self,
        num_in,
        num_mid,
        num_out,
        g=1,
        stride=(1, 1, 1),
        first_block=False,
        use_3d=True,
    ):
        super(MF_UNIT, self).__init__()
        num_ix = int(num_mid / 4)
        kt, pt = (3, 1) if use_3d else (1, 0)
        # prepare input
        self.conv_i1 = BN_AC_CONV3D(
            num_in=num_in, num_filter=num_ix, kernel=(1, 1, 1), pad=(0, 0, 0)
        )
        self.conv_i2 = BN_AC_CONV3D(
            num_in=num_ix, num_filter=num_in, kernel=(1, 1, 1), pad=(0, 0, 0)
        )
        # main part
        self.conv_m1 = BN_AC_CONV3D(
            num_in=num_in,
            num_filter=num_mid,
            kernel=(kt, 3, 3),
            pad=(pt, 1, 1),
            stride=stride,
            g=g,
        )
        if first_block:
            self.conv_m2 = BN_AC_CONV3D(
                num_in=num_mid, num_filter=num_out, kernel=(1, 1, 1), pad=(0, 0, 0)
            )
        else:
            self.conv_m2 = BN_AC_CONV3D(
                num_in=num_mid, num_filter=num_out, kernel=(1, 3, 3), pad=(0, 1, 1), g=g
            )
        # adapter
        if first_block:
            self.conv_w1 = BN_AC_CONV3D(
                num_in=num_in,
                num_filter=num_out,
                kernel=(1, 1, 1),
                pad=(0, 0, 0),
                stride=stride,
            )

    def forward(self, x):

        h = self.conv_i1(x)
        x_in = x + self.conv_i2(h)

        h = self.conv_m1(x_in)
        h = self.conv_m2(h)

        if hasattr(self, "conv_w1"):
            x = self.conv_w1(x)

        return h + x


class MFNET_3D(nn.Module):
    def __init__(self, num_classes):
        super(MFNET_3D, self).__init__()

        groups = 16
        k_sec = {2: 3, 3: 4, 4: 6, 5: 3}

        # conv1 - x224 (x16)
        conv1_num_out = 16
        self.conv1 = nn.Sequential(
            OrderedDict(
                [
                    (
                        "conv",
                        nn.Conv3d(
                            3,
                            conv1_num_out,
                            kernel_size=(3, 5, 5),
                            padding=(1, 2, 2),
                            stride=(1, 2, 2),
                            bias=False,
                        ),
                    ),
                    ("bn", nn.BatchNorm3d(conv1_num_out)),
                    ("relu", nn.ReLU(inplace=True)),
                ]
            )
        )
        self.maxpool = nn.MaxPool3d(
            kernel_size=(1, 3, 3), stride=(1, 2, 2), padding=(0, 1, 1)
        )

        # conv2 - x56 (x8)
        num_mid = 96
        conv2_num_out = 96
        self.conv2 = nn.Sequential(
            OrderedDict(
                [
                    (
                        "B%02d" % i,
                        MF_UNIT(
                            num_in=conv1_num_out if i == 1 else conv2_num_out,
                            num_mid=num_mid,
                            num_out=conv2_num_out,
                            stride=(2, 1, 1) if i == 1 else (1, 1, 1),
                            g=groups,
                            first_block=(i == 1),
                        ),
                    )
                    for i in range(1, k_sec[2] + 1)
                ]
            )
        )

        # conv3 - x28 (x8)
        num_mid *= 2
        conv3_num_out = 2 * conv2_num_out
        self.conv3 = nn.Sequential(
            OrderedDict(
                [
                    (
                        "B%02d" % i,
                        MF_UNIT(
                            num_in=conv2_num_out if i == 1 else conv3_num_out,
                            num_mid=num_mid,
                            num_out=conv3_num_out,
                            stride=(1, 2, 2) if i == 1 else (1, 1, 1),
                            g=groups,
                            first_block=(i == 1),
                        ),
                    )
                    for i in range(1, k_sec[3] + 1)
                ]
            )
        )

        # conv4 - x14 (x8)
        num_mid *= 2
        conv4_num_out = 2 * conv3_num_out
        self.conv4 = nn.Sequential(
            OrderedDict(
                [
                    (
                        "B%02d" % i,
                        MF_UNIT(
                            num_in=conv3_num_out if i == 1 else conv4_num_out,
                            num_mid=num_mid,
                            num_out=conv4_num_out,
                            stride=(1, 2, 2) if i == 1 else (1, 1, 1),
                            g=groups,
                            first_block=(i == 1),
                        ),
                    )
                    for i in range(1, k_sec[4] + 1)
                ]
            )
        )

        # conv5 - x7 (x8)
        num_mid *= 2
        conv5_num_out = 2 * conv4_num_out
        self.conv5 = nn.Sequential(
            OrderedDict(
                [
                    (
                        "B%02d" % i,
                        MF_UNIT(
                            num_in=conv4_num_out if i == 1 else conv5_num_out,
                            num_mid=num_mid,
                            num_out=conv5_num_out,
                            stride=(1, 2, 2) if i == 1 else (1, 1, 1),
                            g=groups,
                            first_block=(i == 1),
                        ),
                    )
                    for i in range(1, k_sec[5] + 1)
                ]
            )
        )

        # final
        self.tail = nn.Sequential(
            OrderedDict(
                [("bn", nn.BatchNorm3d(conv5_num_out)), ("relu", nn.ReLU(inplace=True))]
            )
        )

        self.globalpool = nn.Sequential(
            OrderedDict(
                [
                    ("avg", nn.AvgPool3d(kernel_size=(8, 7, 7), stride=(1, 1, 1))),
                    # ('dropout', nn.Dropout(p=0.5)), only for fine-tuning
                ]
            )
        )
        self.classifier = nn.Linear(conv5_num_out, num_classes)

        #############
        # Initialization
        xavier(net=self)

    def forward(self, x):
        assert x.shape[2] == 16

        h = self.conv1(x)  # x224 -> x112
        h = self.maxpool(h)  # x112 ->  x56

        h = self.conv2(h)  # x56 ->  x56
        h = self.conv3(h)  # x56 ->  x28
        h = self.conv4(h)  # x28 ->  x14
        h = self.conv5(h)  # x14 ->   x7

        h = self.tail(h)
        h = self.globalpool(h)

        h = h.view(h.shape[0], -1)
        h = self.classifier(h)

        return h


if __name__ == "__main__":

    logging.getLogger().setLevel(logging.DEBUG)
    # ---------
    net = MFNET_3D(num_classes=100)
    data = torch.autograd.Variable(torch.randn(1, 3, 16, 224, 224))
    output = net(data)
    torch.save({"state_dict": net.state_dict()}, "./tmp.pth")
    print(output.shape)
