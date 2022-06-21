# sourced from https://pytorch.org/tutorials/intermediate/speech_command_classification_with_torchaudio_tutorial.html with a little code modified

import torch
import torch.nn.functional as F
import torch.optim as optim
import torchaudio
from tqdm import tqdm
import os

try:
    from . import dataset
except ImportError:
    import dataset

try:
    from . import model
except ImportError:
    import model

_ROOT_DIR = os.path.dirname(os.path.dirname(__file__))
_MODEL_PATH = os.path.join(_ROOT_DIR, "../models/m5.pth")
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
# The transform needs to live on the same device as the model and the data.
transform = torchaudio.transforms.Resample(orig_freq=16000, new_freq=8000)
transform = transform.to(device)


def train(model, epoch, log_interval, train_loader, optimizer, losses, pbar,
    pbar_update):
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
                f"Train Epoch: {epoch} [{batch_idx * len(data)}/{len(train_loader.dataset)} ({100. * batch_idx / len(train_loader):.0f}%)]\tLoss: {loss.item():.6f}")

        # update progress bar
        pbar.update(pbar_update)
        # record loss
        losses.append(loss.item())


def number_of_correct(pred, target):
    # count number of correct predictions
    return pred.squeeze().eq(target).sum().item()


def get_likely_index(tensor):
    # find most likely label index for each element in the batch
    return tensor.argmax(dim=-1)


def test(model, epoch, test_loader, pbar, pbar_update):
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
        # update progress bar
        pbar.update(pbar_update)

    print(
        f"\nTest Epoch: {epoch}\tAccuracy: {correct}/{len(test_loader.dataset)} ({100. * correct / len(test_loader.dataset):.0f}%)\n")


def main():
    log_interval = 20
    n_epoch = 1
    batch_size = 256
    if device == "cuda":
        num_workers = 1
        pin_memory = True
    else:
        num_workers = 0
        pin_memory = False
    train_set = dataset.SubsetSC("training")
    test_set = dataset.SubsetSC("testing")
    labels = sorted(list(set(datapoint[2] for datapoint in train_set)))
    print(labels)
    collate_fn = dataset.collate_fn_lambda(labels)
    train_loader = torch.utils.data.DataLoader(
        train_set,
        batch_size=batch_size,
        shuffle=True,
        collate_fn=collate_fn,
        num_workers=num_workers,
        pin_memory=pin_memory,
    )
    test_loader = torch.utils.data.DataLoader(
        test_set,
        batch_size=batch_size,
        shuffle=False,
        drop_last=False,
        collate_fn=collate_fn,
        num_workers=num_workers,
        pin_memory=pin_memory,
    )
    _model = model.M5(n_input=1, n_output=len(labels))
    _model.to(device)
    pbar_update = 1 / (len(train_loader) + len(test_loader))
    losses = []
    optimizer = optim.Adam(_model.parameters(), lr=0.01, weight_decay=0.0001)
    scheduler = optim.lr_scheduler.StepLR(optimizer, step_size=20,
                                          gamma=0.1)  # reduce the learning after 20 epochs by a factor of 10
    with tqdm(total=n_epoch) as pbar:
        for epoch in range(1, n_epoch + 1):
            train(_model, epoch, log_interval, train_loader, optimizer, losses,
                  pbar, pbar_update)
            test(_model, epoch, test_loader, pbar, pbar_update)
            scheduler.step()
    torch.save(_model.state_dict(), _MODEL_PATH)


# Let's plot the training loss versus the number of iteration.
# plt.plot(losses);
# plt.title("training loss");


if __name__ == "__main__":
    main()
