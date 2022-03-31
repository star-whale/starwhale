from pathlib import Path
import io

import torch
from torchvision import transforms
from PIL import Image

from model import Net

ROOTDIR = Path(__file__).parent.parent


class MNISTInference(object):

    def __init__(self, device="cpu") -> None:
        self.model = Net()
        self.device = torch.device(device)

    def _pre(self, input):
        image = Image.open(io.BytesIO(input))
        image = transforms.Compose([
            transforms.ToTensor(),
            transforms.Normalize((0.1307,), (0.3081,))
        ])(image)
        return torch.stack([image]).to(self.device)

    def _post(self, input):
        return input.argmax(1).flatten().tolist()[0]

    def inference(self, data):
        data = self._pre(data)
        output = self.model(data)
        return self._post(output)

    def load(self):
        self.model = Net().to(self.device)
        self.model.load_state_dict(torch.load(str(ROOTDIR / "models/mnist_cnn.pt")))
        self.model.eval()


if __name__ == "__main__":
    mnist = MNISTInference()
    mnist.load()

    tdir = ROOTDIR / "data/test"
    for f in tdir.iterdir():
        output = mnist.inference(f.open("rb").read())
        print(f"{f.name} -> {output}")