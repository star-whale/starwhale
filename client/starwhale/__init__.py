from starwhale.api.job import step, Context, pass_context
from starwhale.version import STARWHALE_VERSION as __version__
from starwhale.base.uri import URI, URIType
from starwhale.api.model import PipelineHandler, PPLResultStorage, PPLResultIterator
from starwhale.api.metric import multi_classification
from starwhale.api.dataset import (
    Line,
    Link,
    Text,
    Audio,
    Image,
    Point,
    Video,
    Binary,
    Dataset,
    Polygon,
    LinkAuth,
    LinkType,
    MIMEType,
    ClassLabel,
    S3LinkAuth,
    BoundingBox,
    BuildExecutor,
    GrayscaleImage,
    get_data_loader,
    LocalFSLinkAuth,
    DefaultS3LinkAuth,
    COCOObjectAnnotation,
    SWDSBinBuildExecutor,
    UserRawBuildExecutor,
)
from starwhale.api.evaluation import Evaluation
from starwhale.core.dataset.tabular import get_dataset_consumption

dataset = Dataset.dataset

__all__ = [
    "__version__",
    "PipelineHandler",
    "multi_classification",
    "Dataset",
    "dataset",
    "URI",
    "URIType",
    "step",
    "pass_context",
    "Context",
    "Evaluation",
    "get_data_loader",
    "Link",
    "LinkAuth",
    "DefaultS3LinkAuth",
    "LocalFSLinkAuth",
    "S3LinkAuth",
    "MIMEType",
    "LinkType",
    "BuildExecutor",  # SWDSBinBuildExecutor alias
    "UserRawBuildExecutor",
    "SWDSBinBuildExecutor",
    "Binary",
    "Text",
    "Line",
    "Point",
    "Polygon",
    "Audio",
    "Video",
    "Image",
    "ClassLabel",
    "BoundingBox",
    "GrayscaleImage",
    "COCOObjectAnnotation",
    "PPLResultStorage",
    "PPLResultIterator",
    "get_dataset_consumption",
]
