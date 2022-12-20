---
title: The `image-net` Dataset
---

## The Image Net Description

- [Homepage](https://image-net.org/challenges/LSVRC/index.php)

## The `image-net` dataset Structure

This dataset is built on [Image Net](https://image-net.org/challenges/LSVRC/index.php)'s validation set which contains 50,000 images.

### Data Fields

- `data`: `starwhale.Image` loaded as bytes array
- `annotations` of type dict:
  - `annotation`: below xml is loaded as dict to `annotation`

```xml
<annotation>
	<folder>val</folder>
	<filename>ILSVRC2012_val_00000001</filename>
	<source>
		<database>ILSVRC_2012</database>
	</source>
	<size>
		<width>500</width>
		<height>375</height>
		<depth>3</depth>
	</size>
	<segmented>0</segmented>
	<object>
		<name>n01751748</name>
		<pose>Unspecified</pose>
		<truncated>0</truncated>
		<difficult>0</difficult>
		<bndbox>
			<xmin>111</xmin>
			<ymin>108</ymin>
			<xmax>441</xmax>
			<ymax>193</ymax>
		</bndbox>
	</object>
</annotation>
```

### Sample
```json
{
	"annotation": {
		"filename": "ILSVRC2012_val_00000001",
		"folder": "val",
		"object": [{
			"bbox_view": "BoundingBox: point:(111, 108), width: 330, height: 85",
			"bndbox": {
				"xmax": "441",
				"xmin": "111",
				"ymax": "193",
				"ymin": "108"
			},
			"difficult": "0",
			"name": "n01751748",
			"pose": "Unspecified",
			"truncated": "0"
		}],
		"segmented": "0",
		"size": {
			"depth": "3",
			"height": "375",
			"width": "500"
		},
		"source": {
			"database": "ILSVRC_2012"
		}
	}
}
```

## Build `image-net` Dataset locally

```shell
 swcli dataset build . --name image-net --handler dataset:do_iter_item
```

## Example

Output the first 1 record of the `image-net` dataset.

```shell
python3 example.py
```
