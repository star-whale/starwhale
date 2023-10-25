import logging


class EvalMetric(object):
    def __init__(self, name, **kwargs):
        self.sum_metric = None
        self.num_inst = None
        self.name = str(name)
        self.reset()

    def update(self, preds, labels, losses):
        raise NotImplementedError()

    def reset(self):
        self.num_inst = 0
        self.sum_metric = 0.0

    def get(self):
        if self.num_inst == 0:
            return self.name, float("nan")
        else:
            return self.name, self.sum_metric / self.num_inst

    def get_name_value(self):
        name, value = self.get()
        if not isinstance(name, list):
            name = [name]
        if not isinstance(value, list):
            value = [value]
        return list(zip(name, value))

    def check_label_shapes(self, preds, labels):
        # raise if the shape is inconsistent
        if isinstance(labels, list) and isinstance(preds, list):
            label_shape, pred_shape = len(labels), len(preds)
        else:
            label_shape, pred_shape = labels.shape[0], preds.shape[0]

        if label_shape != pred_shape:
            raise NotImplementedError("")


class MetricList(EvalMetric):
    """Handle multiple evaluation metric"""

    def __init__(self, *args, name="metric_list"):
        assert all(
            [issubclass(type(x), EvalMetric) for x in args]
        ), f"MetricList input is illegal: {args}"
        self.metrics = [metric for metric in args]
        super(MetricList, self).__init__(name=name)

    def update(self, preds, labels, losses=None):
        preds = [preds] if not isinstance(preds, list) else preds
        labels = [labels] if not isinstance(labels, list) else labels
        losses = [losses] if not isinstance(losses, list) else losses

        for metric in self.metrics:
            metric.update(preds, labels, losses)

    def reset(self):
        if hasattr(self, "metrics"):
            for metric in self.metrics:
                metric.reset()
        else:
            logging.warning("No metric defined.")

    def get(self):
        ouputs = []
        for metric in self.metrics:
            ouputs.append(metric.get())
        return ouputs

    def get_name_value(self):
        ouputs = []
        for metric in self.metrics:
            ouputs.append(metric.get_name_value())
        return ouputs


####################
# COMMON METRICS
####################


class Accuracy(EvalMetric):
    """Computes accuracy classification score."""

    def __init__(self, name="accuracy", topk=1):
        super(Accuracy, self).__init__(name)
        self.topk = topk

    def update(self, preds, labels, losses):
        preds = [preds] if not isinstance(preds, list) else preds
        labels = [labels] if not isinstance(labels, list) else labels

        self.check_label_shapes(preds, labels)
        for pred, label in zip(preds, labels):
            assert (
                self.topk <= pred.shape[1]
            ), f"topk({self.topk}) should no larger than the pred dim({pred.shape[1]})"
            _, pred_topk = pred.topk(self.topk, 1, True, True)

            pred_topk = pred_topk.t()
            correct = pred_topk.eq(label.view(1, -1).expand_as(pred_topk))

            self.sum_metric += float(
                correct.contiguous().view(-1).float().sum(0, keepdim=True).numpy()
            )
            self.num_inst += label.shape[0]


class Loss(EvalMetric):
    """Dummy metric for directly printing loss."""

    def __init__(self, name="loss"):
        super(Loss, self).__init__(name)

    def update(self, preds, labels, losses):
        assert losses is not None, "Loss undefined."
        for loss in losses:
            # print(f"loss is:{loss}, type is:{type(loss)}, metric:{float(loss.numpy().sum())},
            # shape is:{loss.shape}, type is {type(loss.shape)}")
            self.sum_metric += float(loss.numpy().sum())
            self.num_inst += 1  # loss.shape[0]
