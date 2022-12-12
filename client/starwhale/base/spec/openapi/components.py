import typing as t

from attrs import define


@define
class Schema:
    """
    https://spec.openapis.org/oas/v3.0.0#schemaObject
    """

    # TODO: support all fields
    type: t.Optional[str] = None
    allOf: t.Optional[t.List["Schema"]] = None
    oneOf: t.Optional[t.List["Schema"]] = None
    anyOf: t.Optional[t.List["Schema"]] = None
    not_: t.Optional["Schema"] = None
    description: t.Optional[str] = None
    format: t.Optional[str] = None  # https://spec.openapis.org/oas/v3.0.0#data-types

    required: t.Optional[t.List[str]] = None
    properties: t.Optional[t.Dict[str, "Schema"]] = None
    items: t.Optional["Schema"] = None


@define
class Header:
    """
    https://spec.openapis.org/oas/v3.0.0#header-object
    """

    description: t.Optional[str] = None
    required: t.Optional[bool] = None
    deprecated: t.Optional[bool] = None
    allowEmptyValue: t.Optional[bool] = None

    style: t.Optional[str] = None
    explode: t.Optional[bool] = None
    allowReserved: t.Optional[bool] = None
    schema: t.Optional[Schema] = None
    example: t.Any = None
    examples: t.Optional[t.Dict[str, t.Any]] = None


@define
class MediaType:
    """
    https://spec.openapis.org/oas/v3.0.0#mediaTypeObject
    """

    schema: t.Optional[Schema] = None
    examples: t.Any = None
    encoding: t.Optional[t.Dict[str, t.Any]] = None


@define
class OpenApiResponse:
    """
    https://spec.openapis.org/oas/v3.0.0#response-object
    """

    description: t.Optional[str] = None
    headers: t.Optional[t.Dict[str, Header]] = None
    content: t.Optional[t.Dict[str, MediaType]] = None


@define
class Parameter(Header):
    """
    https://spec.openapis.org/oas/v3.0.0#parameter-object
    """

    name: t.Optional[str] = None
    in_: t.Optional[str] = None


@define
class RequestBody:
    """
    https://spec.openapis.org/oas/v3.0.0#request-body-object
    """

    description: t.Optional[str] = None
    content: t.Optional[t.Dict[str, MediaType]] = None
    required: t.Optional[bool] = None


# cattrs do not support TypeDict for py37-38, use object instead
# https://github.com/python-attrs/cattrs/issues/296
@define
class SpecComponent:
    headers: t.Optional[t.Dict[str, Header]] = None
    parameters: t.Optional[t.Dict[str, Parameter]] = None
    requestBody: t.Optional[RequestBody] = None
    responses: t.Optional[t.Dict[str, OpenApiResponse]] = None


@define
class OpenApi:
    paths: t.Dict[str, t.Dict[str, SpecComponent]]
    openapi: str
    info: t.Mapping[str, str]

    def to_dict(self) -> t.Any:
        from cattr import GenConverter

        return GenConverter(omit_if_default=True).unstructure(self)
