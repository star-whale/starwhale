import torch
import torch.optim as optim
import torch.nn.functional as F
from model import Net
from torchvision import datasets, transforms
from torch.utils.data import DataLoader
from torch.optim.lr_scheduler import StepLR


def train(
    model: Net,
    device: torch.device,
    data_loader: DataLoader,
    optimizer: optim.Adadelta,
    epoch: int,
    log_interval: int = 10,
) -> None:
    model.train()

    for idx, (data, target) in enumerate(data_loader):
        data, target = data.to(device), target.to(device)

        optimizer.zero_grad()
        output = model(data)
        loss = F.nll_loss(output, target)
        loss.backward()  # type: ignore
        optimizer.step()

        if idx % log_interval == 0:
            print(
                "Train Epoch: {} [{}/{} ({:.0f}%)]\tLoss: {:.6f}".format(
                    epoch,
                    idx * len(data),
                    len(data_loader.dataset.data),  # type: ignore
                    100.0 * idx / len(data_loader),
                    loss.item(),
                )
            )


def main() -> None:
    cuda = torch.cuda.is_available()
    device = torch.device("cuda" if cuda else "cpu")
    torch.manual_seed(1)
    dataset = datasets.MNIST(
        "data",
        train=True,
        download=True,
        transform=transforms.Compose(
            [transforms.ToTensor(), transforms.Normalize((0.1307,), (0.3081,))]
        ),
    )
    data_loader = DataLoader(
        dataset, batch_size=60, num_workers=1, pin_memory=True, shuffle=True
    )

    model = Net().to(device)
    optimizer = optim.Adadelta(model.parameters(), lr=1.0)
    scheduler = StepLR(optimizer, step_size=1, gamma=0.7)

    print("---> start to train...")
    for epoch in range(1, 5):
        train(model, device, data_loader, optimizer, epoch)
        scheduler.step()

    print("--> save model...")
    torch.save(model.state_dict(), "./models/mnist_cnn.pt")


if __name__ == "__main__":
    main()
