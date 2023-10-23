import os
import time
import logging
from pathlib import Path

import torch
import torch.backends.cudnn as cudnn
from model import MFNET_3D
from metric import Loss, Accuracy, MetricList
from sampler import RandomSampling, SequentialSampling
from transform import (
    Resize,
    Compose,
    ToTensor,
    Normalize,
    RandomHLS,
    CenterCrop,
    RandomCrop,
    RandomScale,
    RandomHorizontalFlip,
)
from lr_scheduler import MultiFactorScheduler
from video_iterator import VideoIter
from torch.utils.data import DataLoader

ROOTDIR = Path(__file__).parent.parent


class Callback(object):
    def __init__(self, with_header=False):
        self.with_header = with_header

    def __call__(self):
        raise NotImplementedError("To be implemented")

    def header(self, epoch=None, batch=None):
        str_out = ""
        if self.with_header:
            if epoch is not None:
                str_out += f"Epoch {('[%d]' % epoch).ljust(5, ' ')} "
            if batch is not None:
                str_out += f"Batch {('[%d]' % batch).ljust(6, ' ')} "
        return str_out


class CallbackList(Callback):
    def __init__(self, *args, with_header=True):
        super(CallbackList, self).__init__(with_header=with_header)
        assert all(
            [issubclass(type(x), Callback) for x in args]
        ), f"Callback inputs illegal: {args}"
        self.callbacks = [callback for callback in args]

    def __call__(self, epoch=None, batch=None, silent=False, **kwargs):
        str_out = self.header(epoch, batch)

        for callback in self.callbacks:
            str_out += callback(**kwargs, silent=True) + " "

        if not silent:
            logging.info(str_out)
        return str_out


####################
# CUSTOMIZED CALLBACKS
####################


class SpeedMonitor(Callback):
    def __init__(self, with_header=False):
        super(SpeedMonitor, self).__init__(with_header=with_header)

    def __call__(
        self,
        sample_elapse,
        update_elapse=None,
        epoch=None,
        batch=None,
        silent=False,
        **kwargs,
    ):
        str_out = self.header(epoch, batch)

        if sample_elapse is not None:
            sample_freq = 1.0 / sample_elapse
            if update_elapse is not None:
                update_freq = 1.0 / update_elapse
                str_out += "Speed {: >5.1f} (+{: >2.0f}) sample/sec ".format(
                    sample_freq, update_freq - sample_freq
                )
            else:
                str_out += "Speed {:.2f} sample/sec ".format(sample_freq)

        if not silent:
            logging.info(str_out)
        return str_out


class MetricPrinter(Callback):
    def __init__(self, with_header=False):
        super(MetricPrinter, self).__init__(with_header=with_header)

    def __call__(self, namevals, epoch=None, batch=None, silent=False, **kwargs):
        str_out = self.header(epoch, batch)

        if namevals is not None:
            for i, nameval in enumerate(namevals):
                name, value = nameval[0]
                str_out += "{} = {:.5f}".format(name, value)
                str_out += ", " if i != (len(namevals) - 1) else " "

        if not silent:
            logging.info(str_out)
        return str_out


"""
Static Model
"""


class StaticModel(object):
    def __init__(self, net, criterion=None, model_prefix="", **kwargs):
        if kwargs:
            logging.warning(f"Unknown kwargs: {kwargs}")

        # init params
        self.net = net
        self.model_prefix = model_prefix
        self.criterion = criterion

    def load_state(self, state_dict, strict=False):
        if strict:
            self.net.load_state_dict(state_dict=state_dict)
        else:
            # customized partialy load function
            net_state_keys = list(self.net.state_dict().keys())
            for name, param in state_dict.items():
                if name in self.net.state_dict().keys():
                    dst_param_shape = self.net.state_dict()[name].shape
                    if param.shape == dst_param_shape:
                        self.net.state_dict()[name].copy_(param.view(dst_param_shape))
                        net_state_keys.remove(name)
            # indicating missed keys
            if net_state_keys:
                logging.warning(f">> Failed to load: {net_state_keys}")
                return False
        return True

    def get_checkpoint_path(self, epoch):
        assert self.model_prefix, "model_prefix undefined!"

        return "{}_ep-{:04d}.pth".format(self.model_prefix, epoch)

    def load_checkpoint(self, epoch, optimizer=None):
        load_path = self.get_checkpoint_path(epoch)
        assert os.path.exists(
            load_path
        ), f"Failed to load: {load_path} (file not exist)"

        checkpoint = torch.load(load_path)

        all_params_matched = self.load_state(checkpoint["state_dict"], strict=True)

        if optimizer:
            if "optimizer" in checkpoint.keys() and all_params_matched:
                optimizer.load_state_dict(checkpoint["optimizer"])
                logging.info(
                    f"Model & Optimizer states are resumed from: `{load_path}'"
                )
            else:
                logging.warning(
                    f">> Failed to load optimizer state from: `{load_path}'"
                )
        else:
            logging.info(f"Only model state resumed from: `{load_path}'")

        if "epoch" in checkpoint.keys():
            if checkpoint["epoch"] != epoch:
                logging.warning(
                    f">> Epoch information inconsistant: {checkpoint['epoch']} vs {epoch}"
                )

    def save_checkpoint(self, epoch, optimizer_state=None):
        save_path = self.get_checkpoint_path(epoch)
        save_folder = os.path.dirname(save_path)

        if not os.path.exists(save_folder):
            logging.debug(f"mkdir {save_folder}")
            os.makedirs(save_folder)

        if not optimizer_state:
            torch.save({"epoch": epoch, "state_dict": self.net.state_dict()}, save_path)
            logging.info(f"Checkpoint (only model) saved to: {save_path}")
        else:
            torch.save(
                {
                    "epoch": epoch,
                    "state_dict": self.net.state_dict(),
                    "optimizer": optimizer_state,
                },
                save_path,
            )
            logging.info(f"Checkpoint (model & optimizer) saved to: {save_path}")

    def forward(self, data, target):
        """typical forward function with:
        single output and single loss
        """

        if torch.cuda.is_available():
            data = data.float().cuda()
            target = target.cuda()
        else:
            data = data.float()

        if self.net.training:
            input_var = torch.autograd.Variable(data, requires_grad=False)
            target_var = torch.autograd.Variable(target, requires_grad=False)
        else:
            input_var = torch.autograd.Variable(data, volatile=True)
            target_var = torch.autograd.Variable(target, volatile=True)

        output = self.net(input_var)
        if (
            hasattr(self, "criterion")
            and self.criterion is not None
            and target is not None
        ):
            loss = self.criterion(output, target_var)
        else:
            loss = None
        return [output], [loss]


"""
Dynamic model that is able to update itself
"""


class DynamicModel(StaticModel):
    def __init__(
        self,
        net,
        criterion,
        model_prefix="",
        step_callback=None,
        step_callback_freq=50,
        epoch_callback=None,
        save_checkpoint_freq=1,
        opt_batch_size=None,
        **kwargs,
    ):
        # load parameters
        if kwargs:
            logging.warning(f"Unknown kwargs: {kwargs}")

        super(DynamicModel, self).__init__(
            net, criterion=criterion, model_prefix=model_prefix
        )

        # load optional arguments
        # - callbacks
        self.callback_kwargs = {
            "epoch": None,
            "batch": None,
            "sample_elapse": None,
            "update_elapse": None,
            "epoch_elapse": None,
            "namevals": None,
            "optimizer_dict": None,
        }

        if not step_callback:
            step_callback = CallbackList(SpeedMonitor(), MetricPrinter())
        if not epoch_callback:
            epoch_callback = lambda **kwargs: None

        self.step_callback = step_callback
        self.step_callback_freq = step_callback_freq
        self.epoch_callback = epoch_callback
        self.save_checkpoint_freq = save_checkpoint_freq
        self.batch_size = opt_batch_size

    """
    In order to customize the callback function,
    you will have to overwrite the functions below
    """

    def step_end_callback(self):
        logging.debug(f"Step {self.i_step} finished!")
        self.step_callback(**self.callback_kwargs)

    def epoch_end_callback(self):
        self.epoch_callback(**self.callback_kwargs)
        if self.callback_kwargs["epoch_elapse"] is not None:
            logging.info(
                "Epoch [{:d}]   time cost: {:.2f} sec ({:.2f} h)".format(
                    self.callback_kwargs["epoch"],
                    self.callback_kwargs["epoch_elapse"],
                    self.callback_kwargs["epoch_elapse"] / 3600.0,
                )
            )
        # 0 or best result(least loss) ?????? by gxx
        if (
            self.callback_kwargs["epoch"] == 0
            or ((self.callback_kwargs["epoch"] + 1) % self.save_checkpoint_freq) == 0
        ):
            self.save_checkpoint(
                epoch=self.callback_kwargs["epoch"] + 1,
                optimizer_state=self.callback_kwargs["optimizer_dict"],
            )

    """
    Learning rate
    """

    def adjust_learning_rate(self, lr, optimizer):
        for param_group in optimizer.param_groups:
            if "lr_mult" in param_group:
                lr_mult = param_group["lr_mult"]
            else:
                lr_mult = 1.0
            param_group["lr"] = lr * lr_mult

    """
    Optimization
    """

    def fit(
        self,
        train_iter,
        optimizer,
        lr_scheduler,
        eval_iter=None,
        metrics=None,
        epoch_start=0,
        epoch_end=10000,
        **kwargs,
    ):
        """
        checking
        """
        if kwargs:
            logging.warning(f"Unknown kwargs: {kwargs}")

        """
        start the main loop
        """
        for i_epoch in range(epoch_start, epoch_end):
            self.callback_kwargs["epoch"] = i_epoch
            epoch_start_time = time.time()

            ###########
            # 1] TRAINING
            ###########
            metrics.reset()
            self.net.train()
            sum_sample_inst = 0
            sum_sample_elapse = 0.0
            sum_update_elapse = 0
            batch_start_time = time.time()
            logging.info(f"Start epoch {i_epoch}:")
            for i_batch, (data, target) in enumerate(train_iter):
                self.callback_kwargs["batch"] = i_batch

                update_start_time = time.time()

                # [forward] making next step
                outputs, losses = self.forward(data, target)

                # [backward]
                optimizer.zero_grad()
                for loss in losses:
                    loss.backward()
                self.adjust_learning_rate(optimizer=optimizer, lr=lr_scheduler.update())
                optimizer.step()

                # [evaluation] update train metric
                metrics.update(
                    [output.data.cpu() for output in outputs],
                    target.cpu(),
                    [loss.data.cpu() for loss in losses],
                )

                # timing each batch
                sum_sample_elapse += time.time() - batch_start_time
                sum_update_elapse += time.time() - update_start_time
                batch_start_time = time.time()
                sum_sample_inst += data.shape[0]

                if (i_batch % self.step_callback_freq) == 0:
                    # retrive eval results and reset metic
                    self.callback_kwargs["namevals"] = metrics.get_name_value()
                    metrics.reset()
                    # speed monitor
                    self.callback_kwargs["sample_elapse"] = (
                        sum_sample_elapse / sum_sample_inst
                    )
                    self.callback_kwargs["update_elapse"] = (
                        sum_update_elapse / sum_sample_inst
                    )
                    sum_update_elapse = 0
                    sum_sample_elapse = 0
                    sum_sample_inst = 0
                    # callbacks
                    self.step_end_callback()

            ###########
            # 2] END OF EPOCH
            ###########
            self.callback_kwargs["epoch_elapse"] = time.time() - epoch_start_time
            self.callback_kwargs["optimizer_dict"] = optimizer.state_dict()
            self.epoch_end_callback()

            ###########
            # 3] Evaluation
            ###########
            if (eval_iter is not None) and (
                (i_epoch + 1) % max(1, int(self.save_checkpoint_freq / 2))
            ) == 0:
                logging.info(f"Start evaluating epoch {i_epoch}:")

                metrics.reset()
                self.net.eval()
                sum_sample_elapse = 0.0
                sum_sample_inst = 0
                sum_forward_elapse = 0.0
                batch_start_time = time.time()
                for i_batch, (data, target) in enumerate(eval_iter):
                    self.callback_kwargs["batch"] = i_batch

                    forward_start_time = time.time()

                    outputs, losses = self.forward(data, target)

                    metrics.update(
                        [output.data.cpu() for output in outputs],
                        target.cpu(),
                        [loss.data.cpu() for loss in losses],
                    )

                    sum_forward_elapse += time.time() - forward_start_time
                    sum_sample_elapse += time.time() - batch_start_time
                    batch_start_time = time.time()
                    sum_sample_inst += data.shape[0]

                # evaluation callbacks
                self.callback_kwargs["sample_elapse"] = (
                    sum_sample_elapse / sum_sample_inst
                )
                self.callback_kwargs["update_elapse"] = (
                    sum_forward_elapse / sum_sample_inst
                )
                self.callback_kwargs["namevals"] = metrics.get_name_value()
                self.step_end_callback()

        logging.info("Optimization done!")


def get_ucf101(
    data_root="/data",
    clip_length=8,
    train_interval=2,
    val_interval=2,
    mean=None,
    std=None,
    seed=0,
):
    mean = None or [0.485, 0.456, 0.406]
    std = None or [0.229, 0.224, 0.225]
    """data iter for ucf-101"""
    logging.debug(
        f"VideoIter:: clip_length = {clip_length}, "
        f"interval = [train: {train_interval}, val: {val_interval}], seed = {seed}"
    )

    normalize = Normalize(mean=mean, std=std)

    train_sampler = RandomSampling(
        num=clip_length, interval=train_interval, speed=[1.0, 1.0], seed=(seed + 0)
    )
    train = VideoIter(
        video_prefix=os.path.join(data_root, "UCF-101"),
        txt_list=os.path.join(data_root, "train_list.txt"),
        sampler=train_sampler,
        force_color=True,
        video_transform=Compose(
            [
                RandomScale(
                    make_square=True, aspect_ratio=[0.8, 1.0 / 0.8], slen=[224, 288]
                ),
                RandomCrop((224, 224)),  # insert a resize if needed
                RandomHorizontalFlip(),
                RandomHLS(vars=[15, 35, 25]),
                ToTensor(),
                normalize,
            ],
            aug_seed=(seed + 1),
        ),
        name="train",
        shuffle_list_seed=(seed + 2),
    )

    val_sampler = SequentialSampling(
        num=clip_length, interval=val_interval, fix_cursor=True, shuffle=True
    )
    val = VideoIter(
        video_prefix=os.path.join(data_root, "UCF-101"),
        txt_list=os.path.join(data_root, "validation_list.txt"),
        sampler=val_sampler,
        force_color=True,
        video_transform=Compose(
            [
                Resize((256, 256)),
                CenterCrop((224, 224)),
                ToTensor(),
                normalize,
            ]
        ),
        name="test",
    )

    return train, val


def train_model(
    start_with_pretrained=False,
    num_workers=16,
    resume_epoch=-1,
    batch_size=4,
    save_frequency=1,
    lr_base=0.01,
    lr_factor=0.1,
    lr_steps=None,
    end_epoch=100,
    fine_tune=False,
):
    lr_steps = lr_steps or [400000, 800000]
    # data iterator
    iter_seed = 101 + max(0, resume_epoch) * 100

    train, val = get_ucf101(
        data_root=str(ROOTDIR / "data"),
        clip_length=16,
        train_interval=2,
        val_interval=2,
        mean=[124 / 255, 117 / 255, 104 / 255],
        std=[1 / (0.0167 * 255)] * 3,
        seed=iter_seed,
    )

    train_iter = DataLoader(
        train,
        batch_size=batch_size,
        shuffle=True,
        num_workers=num_workers,
        pin_memory=False,
    )

    eval_iter = DataLoader(
        val,
        batch_size=2 * batch_size,
        shuffle=False,
        num_workers=num_workers,
        pin_memory=False,
    )
    # load model
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model = MFNET_3D(num_classes=101)
    model.to(device)

    # init from 2d
    # state_dict_2d = torch.load(str(ROOTDIR / "models/pretrained/MFNet2D_ImageNet1k-0000.pth"))
    # init_3d_from_2d_dict(net=model, state_dict=state_dict_2d, method="inflation")

    # wrapper (dynamic model)
    dynamic_model = DynamicModel(
        net=model,
        criterion=torch.nn.CrossEntropyLoss().cuda()
        if torch.cuda.is_available()
        else torch.nn.CrossEntropyLoss(),
        model_prefix=str(ROOTDIR / "models/log"),
        step_callback_freq=50,
        save_checkpoint_freq=save_frequency,
        opt_batch_size=batch_size,  # optional
    )

    # config optimization
    param_base_layers = []
    param_new_layers = []
    name_base_layers = []
    for name, param in dynamic_model.net.named_parameters():
        if fine_tune:
            if name.startswith("classifier"):
                param_new_layers.append(param)
            else:
                param_base_layers.append(param)
                name_base_layers.append(name)
        else:
            param_new_layers.append(param)

    if name_base_layers:
        out = "['" + "', '".join(name_base_layers) + "']"
        logging.info(
            f"Optimizer:: >> reducing the learning rate of {len(name_base_layers)} "
            f"params: {out if len(out) < 300 else out[0:150] + ' ... ' + out[-150:]}"
        )
    if torch.cuda.is_available():
        dynamic_model.net = torch.nn.DataParallel(dynamic_model.net).cuda()
    else:
        dynamic_model.net = torch.nn.DataParallel(dynamic_model.net).cpu()

    optimizer = torch.optim.SGD(
        [
            {"params": param_base_layers, "lr_mult": 0.2},
            {"params": param_new_layers, "lr_mult": 1.0},
        ],
        lr=lr_base,
        momentum=0.9,
        weight_decay=0.0001,
        nesterov=True,
    )

    # resume training: model and optimizer
    if resume_epoch < 0:
        epoch_start = 0
        step_counter = 0
        if start_with_pretrained:
            # load params from pretrained 3d network
            checkpoint = torch.load(
                str(ROOTDIR / "models/pretrained/MFNet3D_UCF-101_Split-1_96.3.pth")
            )
            dynamic_model.load_state(checkpoint["state_dict"], strict=False)
    else:
        dynamic_model.load_checkpoint(epoch=resume_epoch, optimizer=optimizer)
        epoch_start = resume_epoch
        step_counter = epoch_start * train_iter.__len__()

    # set learning rate scheduler
    num_worker = 1

    lr_scheduler = MultiFactorScheduler(
        base_lr=lr_base,
        steps=[int(x / (batch_size * num_worker)) for x in lr_steps],
        factor=lr_factor,
        step_counter=step_counter,
    )
    # define evaluation metric
    metrics = MetricList(
        Loss(name="loss-ce"),
        Accuracy(name="top1", topk=1),
        Accuracy(name="top5", topk=5),
    )
    # enable cudnn tune
    cudnn.benchmark = True

    dynamic_model.fit(
        train_iter=train_iter,
        eval_iter=eval_iter,
        optimizer=optimizer,
        lr_scheduler=lr_scheduler,
        metrics=metrics,
        epoch_start=epoch_start,
        epoch_end=end_epoch,
    )


if __name__ == "__main__":
    train_model()
