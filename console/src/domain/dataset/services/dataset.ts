import axios from 'axios'
import { IListQuerySchema, IListSchema } from '@/domain/base/schemas/list'
import {
    IDatasetSchema,
    IDatasetDetailSchema,
    IDatasetTreeSchema,
    ICreateDatasetQuerySchema,
    IDatasetTaskBuildListSchema,
} from '../schemas/dataset'

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

export async function fetchDatasetTree(projectId: string): Promise<IDatasetTreeSchema[]> {
    const resp = await axios.get<IDatasetTreeSchema[]>(`/api/v1/project/${projectId}/dataset-tree`)
    return resp.data
}

export async function createDataset(
    projectId: string,
    datasetName: string,
    data: ICreateDatasetQuerySchema
): Promise<any> {
    const resp = await axios.post<IDatasetTreeSchema[]>(
        `/api/v1/project/${projectId}/dataset/${datasetName}/build`,
        data
    )
    return resp.data
}

export async function removeDataset(projectId: string, datasetId: string): Promise<any> {
    const resp = await axios.delete(`/api/v1/project/${projectId}/dataset/${datasetId}`)
    return resp.data
}

export async function fetchDatasetBuildList(
    projectId: string,
    query: Partial<IListQuerySchema> & { status: 'CREATED' | 'BUILDING' | 'SUCCESS' | 'FAILED' }
): Promise<IListSchema<IDatasetTaskBuildListSchema>> {
    const resp = await axios.get<IDatasetTaskBuildListSchema>(`/api/v1/project/${projectId}/dataset/build/list`, {
        params: query,
    })
    return resp.data
}

export async function fetchDatasetTaskOfflineLogFiles(datasetName: string, taskId: string): Promise<any> {
    const resp = await axios.get<string[]>(`/api/v1/log/offline/dataset/${datasetName}/build/${taskId}`)
    return resp.data
}
