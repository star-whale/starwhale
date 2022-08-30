import axios from 'axios'
import { QueryTableRequest, RecordListVO, ScanTableRequest } from '../schemas/datastore'

export async function queryTable(query: QueryTableRequest): Promise<RecordListVO> {
    const resp = await axios.post<RecordListVO>('/api/v1/datastore/queryTable', query)
    return resp.data
}

export async function scanTable(query: ScanTableRequest): Promise<RecordListVO> {
    const resp = await axios.post<RecordListVO>('/api/v1/datastore/scanTable', query)
    return resp.data
}
