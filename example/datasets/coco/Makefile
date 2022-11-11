.POHNY: raw
raw:
	mkdir -p data
	[ -f data/panoptic_annotations_trainval2017.zip ] || wget wget http://images.cocodataset.org/annotations/panoptic_annotations_trainval2017.zip -O data/panoptic_annotations_trainval2017.zip
	[ -f data/val2017.zip ] || wget http://images.cocodataset.org/zips/val2017.zip -O data/val2017.zip
	[ -d data/val2017 ] || unzip data/val2017.zip -d data
	[ -d data/annotations ] || unzip data/panoptic_annotations_trainval2017.zip -d data
	[ -d data/annotations/panoptic_val2017 ] || unzip data/annotations/panoptic_val2017.zip -d data/annotations
