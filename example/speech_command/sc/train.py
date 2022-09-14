# sourced from https://pytorch.org/tutorials/intermediate/speech_command_classification_with_torchaudio_tutorial.html with a little code modified

import os

import model
import torch
import torchaudio
import torch.optim as optim
import torch.nn.functional as F
from torchaudio.datasets import SPEECHCOMMANDS

ROOT_DIR = os.path.dirname(os.path.dirname(__file__))
DATA_DIR = os.path.join(ROOT_DIR, "data")
MODEL_PATH = os.path.join(ROOT_DIR, "models/m5.pth")

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
# The transform needs to live on the same device as the model and the data.
transform = torchaudio.transforms.Resample(orig_freq=16000, new_freq=8000)
transform = transform.to(device)


def pad_sequence(batch):
    # Make all tensor in a batch the same length by padding with zeros
    batch = [item.t() for item in batch]
    batch = torch.nn.utils.rnn.pad_sequence(batch, batch_first=True, padding_value=0.0)
    return batch.permute(0, 2, 1)


def label_to_index_fn(labels):
    return lambda word: torch.tensor(labels.index(word))


def collate_fn_lambda(labels):
    label_to_index = label_to_index_fn(labels)
    return lambda batch: collate_fn(batch, label_to_index)


def collate_fn(batch, label_to_index):
    # A data tuple has the form:
    # waveform, sample_rate, label, speaker_id, utterance_number
    tensors, targets = [], []

    # Gather in lists, and encode labels as indices
    for waveform, _, label, *_ in batch:
        tensors += [waveform]
        targets += [label_to_index(label)]

    # Group the list of tensors into a batched tensor
    tensors = pad_sequence(tensors)
    targets = torch.stack(targets)

    return tensors, targets


class SubsetSC(SPEECHCOMMANDS):
    def __init__(self, subset: str = ""):
        super().__init__(DATA_DIR, download=True)

        def load_list(filename):
            filepath = os.path.join(self._path, filename)
            with open(filepath) as fileobj:
                return [
                    os.path.normpath(os.path.join(self._path, line.strip()))
                    for line in fileobj
                ]

        if subset == "validation":
            self._walker = load_list("validation_list.txt")
        elif subset == "testing":
            self._walker = load_list("testing_list.txt")
        elif subset == "training":
            excludes = load_list("validation_list.txt") + load_list("testing_list.txt")
            excludes = set(excludes)
            self._walker = [w for w in self._walker if w not in excludes]


def train(model, epoch, log_interval, train_loader, optimizer, losses):
    model.train()
    for batch_idx, (data, target) in enumerate(train_loader):

        data = data.to(device)
        target = target.to(device)

        # apply transform and model on whole batch directly on device
        data = transform(data)
        output = model(data)

        # negative log-likelihood for a tensor of size (batch x 1 x n_output)
        loss = F.nll_loss(output.squeeze(), target)

        optimizer.zero_grad()
        loss.backward()
        optimizer.step()

        # print training stats
        if batch_idx % log_interval == 0:
            print(
                f"Train Epoch: {epoch} [{batch_idx * len(data)}/{len(train_loader.dataset)} ({100. * batch_idx / len(train_loader):.0f}%)]\tLoss: {loss.item():.6f}"
            )
        losses.append(loss.item())


def number_of_correct(pred, target):
    # count number of correct predictions
    return pred.squeeze().eq(target).sum().item()


def get_likely_index(tensor):
    # find most likely label index for each element in t
    return tensor.argmax(dim=-1)


def test(model, epoch, test_loader):
    model.eval()
    correct = 0
    for data, target in test_loader:
        data = data.to(device)
        target = target.to(device)

        # apply transform and model on whole batch directly on device
        data = transform(data)
        output = model(data)

        pred = get_likely_index(output)
        correct += number_of_correct(pred, target)

    print(
        f"\nTest Epoch: {epoch}\tAccuracy: {correct}/{len(test_loader.dataset)} ({100. * correct / len(test_loader.dataset):.0f}%)\n"
    )


def main():
    log_interval = 20
    n_epoch = 10
    batch_size = 256
    if device == "cuda":
        num_workers = 1
        pin_memory = True
    else:
        num_workers = 0
        pin_memory = False
    print("prepare training and testing dataset...")
    train_set = SubsetSC("training")
    test_set = SubsetSC("testing")
    labels = sorted(list(set(datapoint[2] for datapoint in train_set)))
    print(f"labels: {labels}")

    _collate_fn = collate_fn_lambda(labels)
    train_loader = torch.utils.data.DataLoader(
        train_set,
        batch_size=batch_size,
        shuffle=True,
        collate_fn=_collate_fn,
        num_workers=num_workers,
        pin_memory=pin_memory,
    )
    test_loader = torch.utils.data.DataLoader(
        test_set,
        batch_size=batch_size,
        shuffle=False,
        drop_last=False,
        collate_fn=_collate_fn,
        num_workers=num_workers,
        pin_memory=pin_memory,
    )
    _model = model.M5(n_input=1, n_output=len(labels))
    _model.to(device)
    losses = []
    optimizer = optim.Adam(_model.parameters(), lr=0.01, weight_decay=0.0001)
    scheduler = optim.lr_scheduler.StepLR(
        optimizer, step_size=20, gamma=0.1
    )  # reduce the learning after 20 epochs by a factor of 10
    for epoch in range(1, n_epoch + 1):
        print(f"start to train {epoch}...")
        train(
            _model,
            epoch,
            log_interval,
            train_loader,
            optimizer,
            losses,
        )
        print(f"-->test epoch-{epoch}")
        test(_model, epoch, test_loader)
        scheduler.step()
    torch.save(_model.state_dict(), MODEL_PATH)


if __name__ == "__main__":
    main()
