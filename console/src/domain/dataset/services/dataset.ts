import axios from 'axios'
import { ICreateDatasetSchema, IDatasetSchema, IDatasetDetailSchema } from '../schemas/dataset'
import { IListQuerySchema, IListSchema } from '@/domain/base/schemas/list'

export async function listDatasets(projectId: string, query: IListQuerySchema): Promise<IListSchema<IDatasetSchema>> {
    const resp = await axios.get<IListSchema<IDatasetSchema>>(`/api/v1/project/${projectId}/dataset`, {
        params: query,
    })
    return resp.data
}

export async function fetchDataset(projectId: string, datasetId: string): Promise<any> {
    const resp = await axios.get<IDatasetDetailSchema>(`/api/v1/project/${projectId}/dataset/${datasetId}`)
    return resp.data
}

export async function createDataset(projectId: string, data: ICreateDatasetSchema): Promise<IDatasetSchema> {
    var bodyFormData = new FormData()
    bodyFormData.append('datasetName', data.datasetName)
    bodyFormData.append('importPath', data.importPath ?? '')
    if (data.zipFile && data.zipFile.length > 0) bodyFormData.append('zipFile', data.zipFile[0] as File)

    const resp = await axios({
        method: 'post',
        url: `/api/v1/project/${projectId}/dataset`,
        data: bodyFormData,
        headers: { 'Content-Type': 'multipart/form-data' },
    })
    return resp.data
}
