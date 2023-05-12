import glob
import typing as t
import os.path

from fastapi import APIRouter
from pydantic import Field, BaseModel

from starwhale.api._impl import wrapper
from starwhale.utils.config import SWCliConfigMixed
from starwhale.web.response import success, SuccessResp
from starwhale.api._impl.data_store import SwType, _get_type, TableDesc, LocalDataStore

router = APIRouter()
prefix = "datastore"


class ListTablesRequest(BaseModel):
    prefix: str


class Filter(BaseModel):
    operator: str
    operands: t.List[t.Dict[str, str]]


class QueryTableRequest(BaseModel):
    table_name: str = Field(alias="tableName")
    filter: t.Optional[Filter]
    limit: int
    raw_result: t.Optional[bool] = Field(alias="rawResult")
    encode_with_type: t.Optional[bool] = Field(alias="encodeWithType")


@router.post("/listTables")
def list_tables(request: ListTablesRequest) -> SuccessResp:
    # TODO: use datastore builtin function
    root = str(SWCliConfigMixed().datastore_dir)
    path = os.path.join(root, request.prefix)
    files = glob.glob(f"{path}**", recursive=True)
    tables = []
    for f in files:
        if not os.path.isfile(f):
            continue
        p, file = os.path.split(f)
        p = p[len(root) :].lstrip("/")
        table_name = file.split(".sw-datastore.zip")[0]
        tables.append(f"{p}/{table_name}")
    return success({"tables": tables})


@router.post("/queryTable")
def query_table(request: QueryTableRequest) -> SuccessResp:
    eval_id = _is_eval_summary(request)
    if eval_id:
        return _eval_summary(eval_id)

    ds = LocalDataStore.get_instance()
    rows = list(ds.scan_tables([TableDesc(request.table_name)]))
    if request.encode_with_type:
        return _encode_with_type(rows, request.raw_result or False)
    col, rows = _rows_to_type_and_records(rows)
    return success(
        {
            "columnTypes": col,
            "records": rows,
        }
    )


def _encode_with_type(rows: t.Any, raw_result: bool) -> SuccessResp:
    if not rows:
        return success({"records": [], "columnHints": {}})
    ret = []
    column_hints = {k: SwType.encode_schema(_get_type(v)) for k, v in rows[0].items()}
    for row in rows:
        for k, v in row.items():
            typ = _get_type(v)
            row[k] = typ.encode_type_encoded_value(v, raw_result)
        ret.append(row)
    return success({"records": ret, "columnHints": column_hints})


def _is_eval_summary(request: QueryTableRequest) -> t.Union[str, None]:
    if not request.table_name.endswith("/summary"):
        return None
    if not request.filter:
        return None
    op = request.filter.operator
    operands = request.filter.operands
    if op != "EQUAL":
        return None
    if len(operands) != 2 or {"columnName": "sys/id"} not in operands:
        return None

    eval_id = (
        operands[0].get("intValue")
        if operands[1].get("columnName") == "sys/id"
        else operands[1].get("intValue")
    )

    return eval_id


def _eval_summary(eval_id: str) -> SuccessResp:
    evaluation = wrapper.Evaluation(
        eval_id=eval_id,
        project="self",
        instance="local",
    )
    summary = evaluation.get_metrics()
    col, rows = _rows_to_type_and_records(summary)
    return success(
        {
            "columnTypes": col,
            "records": rows,
        }
    )


def _rows_to_type_and_records(rows: t.Union[list, dict]) -> t.Tuple[list, list]:
    if not rows:
        return [], []
    if not isinstance(rows, list):
        rows = [rows]
    encoders = {k: SwType.encode_schema(_get_type(v)) for k, v in rows[0].items()}
    column_types = [v.update({"name": k}) or v for k, v in encoders.items()]
    return column_types, [{k: str(v) for k, v in row.items()} for row in rows]
