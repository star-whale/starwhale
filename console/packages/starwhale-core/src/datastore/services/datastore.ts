import axios from 'axios'
import {
    QueryTableRequest,
    RecordListVo,
    ScanTableRequest,
    ListTablesRequest,
    TableNameListVo,
} from '../schemas/datastore'

export async function queryTable(query: QueryTableRequest): Promise<RecordListVo> {
    const resp = await axios.post<RecordListVo>('/api/v1/datastore/queryTable', query)
    return resp.data
}

export async function scanTable(query: ScanTableRequest): Promise<RecordListVo> {
    const resp = await axios.post<RecordListVo>('/api/v1/datastore/scanTable', query)
    return resp.data
}

export async function listTables(query: ListTablesRequest): Promise<TableNameListVO> {
    const resp = await axios.post<TableNameListVO>('/api/v1/datastore/listTables', query)
    return resp.data
}

export async function exportTable(query: QueryTableRequest): Promise<void> {
    const url = '/api/v1/datastore/queryTable/export'

    await axios({
        method: 'post',
        url,
        data: query,
        responseType: 'blob',
    }).then((response) => {
        const objUrl = window.URL.createObjectURL(response.data)
        const link = document.createElement('a')
        link.href = objUrl
        link.setAttribute('download', 'table.csv')
        document.body.appendChild(link)
        link.click()
    })
}
