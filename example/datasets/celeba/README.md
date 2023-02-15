---
title: The `celeba-align` Dataset
---

## The CelebA Dataset Description

- [Homepage](http://mmlab.ie.cuhk.edu.hk/projects/CelebA.html)

## The `celeba-align` dataset Structure

### Data Fields

The `celeba-align` dataset, doesn't include the `bbox` field which are for un-aligned images.

- `data` of type `dict`:
    - `image`: `starwhale.Image`
    - `identity`: the identity for the image
    - `landmark`: the landmark for eyes/nose/mouse
    - other attrs

Sample data
```json
{
	'image': `starwhale.Image`,
	'identity': '2929',
	'five_o_clock_shadow': '-1',
	'arched_eyebrows': '1',
	'attractive': '1',
	'bags_under_eyes': '-1',
	'bald': '-1',
	'bangs': '-1',
	'big_lips': '-1',
	'big_nose': '-1',
	'black_hair': '-1',
	'blond_hair': '1',
	'blurry': '-1',
	'brown_hair': '-1',
	'bushy_eyebrows': '-1',
	'chubby': '-1',
	'double_chin': '-1',
	'eyeglasses': '-1',
	'goatee': '-1',
	'gray_hair': '-1',
	'heavy_makeup': '1',
	'high_cheekbones': '-1',
	'male': '-1',
	'mouth_slightly_open': '-1',
	'mustache': '-1',
	'narrow_eyes': '-1',
	'no_beard': '1',
	'oval_face': '1',
	'pale_skin': '1',
	'pointy_nose': '-1',
	'receding_hairline': '-1',
	'rosy_cheeks': '-1',
	'sideburns': '-1',
	'smiling': '-1',
	'straight_hair': '-1',
	'wavy_hair': '1',
	'wearing_earrings': '-1',
	'wearing_hat': '-1',
	'wearing_lipstick': '1',
	'wearing_necklace': '1',
	'wearing_necktie': '-1',
	'young': '1',
	'landmark': {
		'left_eye': Point: (68.0, 111.0),
		'right_eye': Point: (108.0, 112.0),
		'left_mouse': Point: (73.0, 152.0),
		'right_mouse': Point: (104.0, 151.0),
		'nose': Point: (88.0, 134.0)
	}
}
```

## Build `celeba-align` Dataset locally

```shell
python3 dataset.py
```

## Example

Output the "000019.jpg" record of the `celeba-align` dataset.

```shell
python3 example.py
```
