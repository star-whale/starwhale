import typing as t
from pathlib import Path

import numpy as np
import torch
from PIL import Image as PILImage
from pycocotools import mask as coco_mask

from starwhale import Link, Image, MIMEType, BoundingBox, COCOObjectAnnotation


class PFPDatasetBuildExecutor:
    def __iter__(self) -> t.Generator:
        root_dir = Path(__file__).parent.parent / "data" / "PennFudanPed"
        names = [p.stem for p in (root_dir / "PNGImages").iterdir()]
        self.object_id = 1
        for idx, name in enumerate(sorted(names)):
            data_fname = f"PNGImages/{name}.png"
            mask_fname = f"PedMasks/{name}_mask.png"
            mask_fpath = root_dir / mask_fname
            data_fpath = root_dir / data_fname
            height, width = self._get_image_shape(data_fpath)
            coco_annotations = self._make_coco_annotations(mask_fpath, data_fname)
            img = Image(
                display_name=name,
                mime_type=MIMEType.PNG,
                shape=(height, width, 3),
                link=Link(
                    data_fpath,
                    size=data_fpath.stat().st_size,
                    with_local_fs_data=True,
                ),
            )
            yield data_fname, {
                "image": img,
                "mask": Image(
                    display_name=name,
                    mime_type=MIMEType.PNG,
                    shape=(height, width, 3),
                    as_mask=True,
                    mask_uri=name,
                    link=Link(
                        mask_fpath,
                        size=mask_fpath.stat().st_size,
                        with_local_fs_data=True,
                    ),
                ),
                "image_id": data_fname,
                "image_height": height,
                "image_width": width,
                "image_name": name,
                "object_nums": len(coco_annotations),
                "annotations": coco_annotations,
            }

    def _get_image_shape(self, fpath: Path) -> t.Tuple[int, int]:
        with PILImage.open(str(fpath)) as f:
            return f.height, f.width

    def _make_coco_annotations(
        self, mask_fpath: Path, image_id: t.Union[int, str]
    ) -> t.List[COCOObjectAnnotation]:
        mask_img = PILImage.open(str(mask_fpath))

        mask = np.array(mask_img)
        object_ids = np.unique(mask)[1:]
        binary_mask = mask == object_ids[:, None, None]
        # TODO: tune permute without pytorch
        binary_mask_tensor = torch.as_tensor(binary_mask, dtype=torch.uint8)
        binary_mask_tensor = (
            binary_mask_tensor.permute(0, 2, 1).contiguous().permute(0, 2, 1)
        )

        coco_annotations = []
        for i in range(0, len(object_ids)):
            _pos = np.where(binary_mask[i])
            _xmin, _ymin = float(np.min(_pos[1])), float(np.min(_pos[0]))
            _xmax, _ymax = float(np.max(_pos[1])), float(np.max(_pos[0]))
            _bbox = BoundingBox(
                x=_xmin, y=_ymin, width=_xmax - _xmin, height=_ymax - _ymin
            )

            rle: t.Dict = coco_mask.encode(binary_mask_tensor[i].numpy())  # type: ignore
            rle["counts"] = rle["counts"].decode("utf-8")

            coco_obj = COCOObjectAnnotation(
                id=self.object_id,
                image_id=image_id,
                category_id=1,  # PennFudan Dataset only has one class-PASPersonStanding
                area=_bbox.width * _bbox.height,
                bbox=_bbox,
                iscrowd=0,  # suppose all instances are not crowd
            )
            coco_obj.segmentation = rle
            coco_annotations.append(coco_obj)
            self.object_id += 1

        return coco_annotations
