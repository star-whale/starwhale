import axios from 'axios'
import { IListQuerySchema, IListSchema } from '@/domain/base/schemas/list'
import { IBuildRecordVo, IDatasetBuildRequest, IDatasetInfoVo, IDatasetViewVo, IPageInfoDatasetVo } from '@/api'

export async function listDatasets(
    projectId: string,
    query: IListQuerySchema & { name?: string }
): Promise<IPageInfoDatasetVo> {
    const resp = await axios.get<IPageInfoDatasetVo>(`/api/v1/project/${projectId}/dataset`, {
        params: query,
    })
    return resp.data
}

export async function fetchDataset(projectId: string, datasetId: string): Promise<IDatasetInfoVo> {
    const resp = await axios.get<IDatasetInfoVo>(`/api/v1/project/${projectId}/dataset/${datasetId}`)
    return resp.data
}

export async function fetchDatasetTree(projectId: string): Promise<IDatasetViewVo[]> {
    const resp = await axios.get<IDatasetViewVo[]>(`/api/v1/project/${projectId}/dataset-tree`)
    return resp.data
}

export async function fetchRecentDatasetTree(projectId: string): Promise<IDatasetViewVo[]> {
    const resp = await axios.get<IDatasetViewVo[]>(`/api/v1/project/${projectId}/recent-dataset-tree`)
    return resp.data
}

export async function createDataset(projectId: string, datasetName: string, data: IDatasetBuildRequest): Promise<any> {
    const resp = await axios.post<IDatasetBuildRequest[]>(
        `/api/v1/project/${projectId}/dataset/${datasetName}/build`,
        data
    )
    return resp.data
}

export async function removeDataset(projectId: string, datasetId: string): Promise<string> {
    const resp = await axios.delete(`/api/v1/project/${projectId}/dataset/${datasetId}`)
    return resp.data
}

export async function fetchDatasetBuildList(
    projectId: string,
    query: Partial<IListQuerySchema> & { status: 'CREATED' | 'BUILDING' | 'SUCCESS' | 'FAILED' }
): Promise<IListSchema<IBuildRecordVo>> {
    const resp = await axios.get<IListSchema<IBuildRecordVo>>(`/api/v1/project/${projectId}/dataset/build/list`, {
        params: query,
    })
    return resp.data
}

export async function fetchDatasetTaskOfflineLogFiles(taskId: string): Promise<any> {
    const resp = await axios.get<string[]>(`/api/v1/log/offline/${taskId}`)
    return resp.data
}

export async function fetchTaskOfflineFileLog(taskId: string, fileId: string): Promise<any> {
    const resp = await axios.get<string>(`/api/v1/log/offline/${taskId}/${fileId}`)
    return resp.data
}
