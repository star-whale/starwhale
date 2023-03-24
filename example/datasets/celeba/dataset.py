import requests

from starwhale import Link, Image, Point, dataset, MIMEType, BoundingBox  # noqa: F401
from starwhale.utils.retry import http_retry

PATH_ROOT = "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/celeba"
IMG_PATH = "img_align_celeba"
IDENTITY_PATH = "identity_CelebA.txt"
ATTR_PATH = "list_attr_celeba.txt"
LANDMARK_PATH = "list_landmarks_align_celeba.txt"


@http_retry
def request_link_text(anno_link):
    return requests.get(anno_link, timeout=10).text


def build_ds():
    ds = dataset("celeba-align")
    identities = request_link_text(f"{PATH_ROOT}/{IDENTITY_PATH}").splitlines()
    attrs = request_link_text(f"{PATH_ROOT}/{ATTR_PATH}").splitlines()
    landmarks = request_link_text(f"{PATH_ROOT}/{LANDMARK_PATH}").splitlines()
    data = {}

    for i in range(202599):
        id_line = identities[i]
        img, identity = id_line.split()  # img,identity
        if img not in data:
            data[img] = {}
        data.get(img)["identity"] = identity
        attr_line = attrs[i + 2]
        (
            img,
            five_o_Clock_Shadow,
            Arched_Eyebrows,
            Attractive,
            Bags_Under_Eyes,
            Bald,
            Bangs,
            Big_Lips,
            Big_Nose,
            Black_Hair,
            Blond_Hair,
            Blurry,
            Brown_Hair,
            Bushy_Eyebrows,
            Chubby,
            Double_Chin,
            Eyeglasses,
            Goatee,
            Gray_Hair,
            Heavy_Makeup,
            High_Cheekbones,
            Male,
            Mouth_Slightly_Open,
            Mustache,
            Narrow_Eyes,
            No_Beard,
            Oval_Face,
            Pale_Skin,
            Pointy_Nose,
            Receding_Hairline,
            Rosy_Cheeks,
            Sideburns,
            Smiling,
            Straight_Hair,
            Wavy_Hair,
            Wearing_Earrings,
            Wearing_Hat,
            Wearing_Lipstick,
            Wearing_Necklace,
            Wearing_Necktie,
            Young,
        ) = attr_line.split()
        if img not in data:
            data[img] = {}
        data.get(img)["five_o_Clock_Shadow"] = five_o_Clock_Shadow
        data.get(img)["Arched_Eyebrows"] = Arched_Eyebrows
        data.get(img)["Attractive"] = Attractive
        data.get(img)["Bags_Under_Eyes"] = Bags_Under_Eyes
        data.get(img)["Bald"] = Bald
        data.get(img)["Bangs"] = Bangs
        data.get(img)["Big_Lips"] = Big_Lips
        data.get(img)["Big_Nose"] = Big_Nose
        data.get(img)["Black_Hair"] = Black_Hair
        data.get(img)["Blond_Hair"] = Blond_Hair
        data.get(img)["Blurry"] = Blurry
        data.get(img)["Brown_Hair"] = Brown_Hair
        data.get(img)["Bushy_Eyebrows"] = Bushy_Eyebrows
        data.get(img)["Chubby"] = Chubby
        data.get(img)["Double_Chin"] = Double_Chin
        data.get(img)["Eyeglasses"] = Eyeglasses
        data.get(img)["Goatee"] = Goatee
        data.get(img)["Gray_Hair"] = Gray_Hair
        data.get(img)["Heavy_Makeup"] = Heavy_Makeup
        data.get(img)["High_Cheekbones"] = High_Cheekbones
        data.get(img)["Male"] = Male
        data.get(img)["Mouth_Slightly_Open"] = Mouth_Slightly_Open
        data.get(img)["Mustache"] = Mustache
        data.get(img)["Narrow_Eyes"] = Narrow_Eyes
        data.get(img)["No_Beard"] = No_Beard
        data.get(img)["Oval_Face"] = Oval_Face
        data.get(img)["Pale_Skin"] = Pale_Skin
        data.get(img)["Pointy_Nose"] = Pointy_Nose
        data.get(img)["Receding_Hairline"] = Receding_Hairline
        data.get(img)["Rosy_Cheeks"] = Rosy_Cheeks
        data.get(img)["Sideburns"] = Sideburns
        data.get(img)["Smiling"] = Smiling
        data.get(img)["Straight_Hair"] = Straight_Hair
        data.get(img)["Wavy_Hair"] = Wavy_Hair
        data.get(img)["Wearing_Earrings"] = Wearing_Earrings
        data.get(img)["Wearing_Hat"] = Wearing_Hat
        data.get(img)["Wearing_Lipstick"] = Wearing_Lipstick
        data.get(img)["Wearing_Necklace"] = Wearing_Necklace
        data.get(img)["Wearing_Necktie"] = Wearing_Necktie
        data.get(img)["Young"] = Young

        ldmk_line = landmarks[i + 2]
        (
            img,
            left_eye_x,
            left_eye_y,
            right_eye_x,
            right_eye_y,
            nose_x,
            nose_y,
            left_mouth_x,
            left_mouth_y,
            right_mouth_x,
            right_mouth_y,
        ) = ldmk_line.split()
        if img not in data:
            data[img] = {}
        data.get(img)["landmark"] = {
            "left_eye": Point(float(left_eye_x), float(left_eye_y)),
            "right_eye": Point(float(right_eye_x), float(right_eye_y)),
            "left_mouse": Point(float(left_mouth_x), float(left_mouth_y)),
            "right_mouse": Point(float(right_mouth_x), float(right_mouth_y)),
            "nose": Point(float(nose_x), float(nose_y)),
        }
        data.get(img)["image"] = Image(
            display_name=img,
            mime_type=MIMEType.JPEG,
            link=Link(f"{PATH_ROOT}/{IMG_PATH}/{img}"),
        )

    for img_id, data in data.items():
        ds.append(
            (
                img_id,
                data,
            )
        )
    ds.commit()
    ds.close()


if __name__ == "__main__":
    build_ds()
