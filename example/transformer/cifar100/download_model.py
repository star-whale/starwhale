from transformers import ViTImageProcessor, ViTForImageClassification

feature_extractor = ViTImageProcessor.from_pretrained("Ahmed9275/Vit-Cifar100")
model = ViTForImageClassification.from_pretrained("Ahmed9275/Vit-Cifar100")
feature_extractor.save_pretrained("./models")
model.save_pretrained("./models")
