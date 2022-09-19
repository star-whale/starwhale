import axios from 'axios'
import {
    QueryTableRequest,
    RecordListVO,
    ScanTableRequest,
    ListTablesRequest,
    TableNameListVO,
} from '../schemas/datastore'

export async function queryTable(query: QueryTableRequest): Promise<RecordListVO> {
    const resp = await axios.post<RecordListVO>('/api/v1/datastore/queryTable', query)
    return resp.data
}

export async function scanTable(query: ScanTableRequest): Promise<RecordListVO> {
    const resp = await axios.post<RecordListVO>('/api/v1/datastore/scanTable', query)
    return resp.data
}

export async function listTables(query: ListTablesRequest): Promise<TableNameListVO> {
    const resp = await axios.post<TableNameListVO>('/api/v1/datastore/listTables', query)
    return resp.data
}
