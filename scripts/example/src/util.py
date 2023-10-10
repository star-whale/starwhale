import io
import random
import string

from starwhale import Image


def random_image() -> bytes:
    try:
        return _random_image_from_pillow()
    except ImportError:
        random_str = "".join(random.sample(string.ascii_lowercase + string.digits, 10))
        return Image(random_str.encode())


def _random_image_from_pillow() -> Image:
    import numpy
    from PIL import Image as PILImage

    pixels = numpy.random.randint(
        low=0, high=256, size=(100, 100, 3), dtype=numpy.uint8
    )
    image_bytes = io.BytesIO()
    PILImage.fromarray(pixels, mode="RGB").save(image_bytes, format="PNG")
    return Image(image_bytes.getvalue())
