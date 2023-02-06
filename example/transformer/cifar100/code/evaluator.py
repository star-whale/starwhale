import io
from pathlib import Path

from PIL import Image as PILImage

from starwhale import Image, PipelineHandler, multi_classification

from transformers import ViTImageProcessor, ViTForImageClassification

ROOTDIR = Path(__file__).parent.parent


class CIFAR100Inference(PipelineHandler):
    def __init__(self) -> None:
        super().__init__()
        self.feature_extractor = ViTImageProcessor.from_pretrained(str(ROOTDIR / "models" ))
        self.model = ViTForImageClassification.from_pretrained(str(ROOTDIR / "models" ))

    def ppl(self, data: dict, **kw):
        with PILImage.open(io.BytesIO(data.pop("image").to_bytes())) as img:
            inputs = self.feature_extractor(images=img, return_tensors="pt")
            logits = self.model(**inputs).logits
        return [logits.argmax(-1).item()]

    @multi_classification(
        confusion_matrix_normalize="all",
        show_hamming_loss=True,
        show_cohen_kappa_score=True,
        show_roc_auc=False,
        all_labels=[i for i in range(0, 100)],
    )
    def cmp(self, ppl_result):
        result, label = [], []
        for _data in ppl_result:
            label.append(_data["ds_data"]["fine_label"])
            result.extend(_data["result"])
        return label, result
