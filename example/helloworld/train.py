import numpy as np
import onnxruntime as rt
from sklearn import datasets
from skl2onnx import to_onnx
from sklearn.neighbors import KNeighborsClassifier
from sklearn.model_selection import train_test_split


def train_knn():
    print("fetch dataset...")
    digits = datasets.load_digits()
    n_samples = len(digits.images)
    data = digits.images.reshape((n_samples, -1))
    data = data.astype(np.float32)
    X_train, X_test, y_train, y_test = train_test_split(
        data, digits.target, test_size=0.5, shuffle=False
    )
    print("train model...")
    knn = KNeighborsClassifier(n_neighbors=5)
    knn.fit(X_train, y_train)
    print("save model into onnx file...")

    onx = to_onnx(knn, X_train)
    model_path = "knn.onnx"
    with open(model_path, "wb") as f:
        f.write(onx.SerializeToString())

    print("test model...")
    sess = rt.InferenceSession(model_path, providers=["CPUExecutionProvider"])
    input_name = sess.get_inputs()[0].name
    label_name = sess.get_outputs()[0].name
    pred_onx = sess.run([label_name], {input_name: X_test.astype(np.float32)})[0]
    print(pred_onx)


if __name__ == "__main__":
    train_knn()
