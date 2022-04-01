from pathlib import Path
import io
import typing as t

import torch
from torchvision import transforms
from PIL import Image

from starwhale.api.model import PipelineHandler

from model import Net

ROOTDIR = Path(__file__).parent.parent
ONE_IMAGE_SIZE = 28 * 28


class MNISTInference(PipelineHandler):

    def __init__(self, device="cpu") -> None:
        super().__init__()
        self.device = torch.device(device)
        self.model = self._load_model(self.device)

    def handle(self, data, batch_size, **kw):
        data = self._pre(data, batch_size)
        output = self.model(data)
        return self._post(output)

    def _pre(self, input: bytes, batch_size: int):
        images = []
        for i in range(0, batch_size):
            #TODO: tune for batch transforms
            start = i * ONE_IMAGE_SIZE
            image = Image.open(io.BytesIO(input[start:(start + ONE_IMAGE_SIZE)]))
            image = transforms.Compose([
                transforms.ToTensor(),
                transforms.Normalize((0.1307,), (0.3081,))
            ])(image)
            images.append(image)
        return torch.stack(images).to(self.device)

    def _post(self, input):
        return input.argmax(1).flatten().tolist()

    def _load_model(self, device):
        model = Net().to(device)
        model.load_state_dict(torch.load(str(ROOTDIR / "models/mnist_cnn.pt")))
        model.eval()
        return model


def local_smoketest():
    mnist = MNISTInference()

    tdir = ROOTDIR / "data/test"
    for f in tdir.iterdir():
        output = mnist.handle(f.open("rb").read(), 1)
        print(f"{f.name} -> {output}")


if __name__ == "__main__":
    local_smoketest()