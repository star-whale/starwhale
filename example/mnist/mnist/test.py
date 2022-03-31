import torch
from torch.utils.data import DataLoader
import torch.nn.functional as F
from torchvision import datasets, transforms

from model import Net


def test(model, device, data_loader):
    model.eval()

    test_loss = 0
    correct = 0

    with torch.no_grad():
        for data, target in data_loader:
            data, target = data.to(device), target.to(device)
            output = model(data)
            test_loss += F.nll_loss(output, target, reduction="sum").item()
            pred = output.argmax(dim=1, keepdim=True)
            correct += pred.eq(target.view_as(pred)).sum().item()

    print('\nTest set: Average loss: {:.4f}, Accuracy: {}/{} ({:.0f}%)\n'.format(
        test_loss / len(data_loader.dataset),
        correct,
        len(data_loader.dataset),
        100. * correct / len(data_loader.dataset)))


def main():
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    torch.manual_seed(1)
    dataset = datasets.MNIST(
        "data", train=False, download=True,
        transform=transforms.Compose([
            transforms.ToTensor(),
            transforms.Normalize((0.1307,), (0.3081,))
        ])
    )
    data_loader = DataLoader(dataset, batch_size=100)
    model = Net().to(device)
    net = torch.load("./models/mnist_cnn.pt")
    model.load_state_dict(net)
    test(model, device, data_loader)


if __name__ == "__main__":
    main()