import io

import flow_vis
import matplotlib.pyplot as plt
from PIL import Image as PILImage

from starwhale import dataset

ds_name = "sintel/version/latest"
ds = dataset(ds_name)
row = ds[1024]
data = row.features
fig, ax = plt.subplots(
    3, 3, figsize=(25, 16), gridspec_kw={"wspace": 0.1, "hspace": 0.1}
)
albedo0 = data["frame0/albedo"].to_bytes()
clean0 = data["frame0/clean"].to_bytes()
final0 = data["frame0/final"].to_bytes()
albedo1 = data["frame1/albedo"].to_bytes()
clean1 = data["frame1/clean"].to_bytes()
final1 = data["frame1/final"].to_bytes()
flow_viz = data["flow_viz"].to_bytes()
flow_bin = data["flow_bin"].to_numpy()
pix_occlusions = data["pix_occlusions"].to_bytes()
with PILImage.open(io.BytesIO(albedo0)) as img1, PILImage.open(
    io.BytesIO(clean0)
) as img2, PILImage.open(io.BytesIO(final0)) as img3, PILImage.open(
    io.BytesIO(albedo1)
) as img4, PILImage.open(
    io.BytesIO(final1)
) as img5, PILImage.open(
    io.BytesIO(clean1)
) as img6, PILImage.open(
    io.BytesIO(flow_viz)
) as img7, PILImage.open(
    io.BytesIO(pix_occlusions)
) as img9:
    img8 = flow_vis.flow_to_color(flow_bin, convert_to_bgr=False)
    ax[0][0].imshow(img1)
    ax[0][1].imshow(img2)
    ax[0][2].imshow(img3)
    ax[1][0].imshow(img4)
    ax[1][1].imshow(img5)
    ax[1][2].imshow(img6)
    ax[2][0].imshow(img7)
    ax[2][1].imshow(img8)
    ax[2][2].imshow(img9)
    fig.show()
