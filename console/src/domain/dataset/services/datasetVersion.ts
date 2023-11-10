import axios from 'axios'
import { IListQuerySchema } from '@/domain/base/schemas/list'
import { IDatasetInfoVo, IPageInfoDatasetVersionVo } from '@/api'

export async function listDatasetVersions(
    projectId: string,
    datasetId: string,
    query: IListQuerySchema
): Promise<IPageInfoDatasetVersionVo> {
    const resp = await axios.get<IPageInfoDatasetVersionVo>(
        `/api/v1/project/${projectId}/dataset/${datasetId}/version`,
        {
            params: query,
        }
    )
    return resp.data
}

export async function fetchDatasetVersion(
    projectId: string,
    datasetId: string,
    datasetVersionId: string
): Promise<IDatasetInfoVo> {
    const resp = await axios.get<IDatasetInfoVo>(
        `/api/v1/project/${projectId}/dataset/${datasetId}?versionUrl=${datasetVersionId}`
    )
    return resp.data
}

export async function revertDatasetVersion(
    projectId: string,
    datasetId: string,
    datasetVersionId: string
): Promise<string> {
    const resp = await axios.post<string>(`/api/v1/project/${projectId}/dataset/${datasetId}/revert`, {
        versionUrl: datasetVersionId,
    })
    return resp.data
}

export async function updateDatasetVersionShared(
    projectId: string,
    datasetId: string,
    datasetVersionId: string,
    shared: boolean
): Promise<string> {
    const resp = await axios.put<string>(
        `/api/v1/project/${projectId}/dataset/${datasetId}/version/${datasetVersionId}/shared?shared=${shared ? 1 : 0}`
    )
    return resp.data
}

export async function addDatasetVersionTag(
    projectId: string,
    datasetId: string,
    datasetVersionId: string,
    tag: string
): Promise<void> {
    const resp = await axios.post<void>(
        `/api/v1/project/${projectId}/dataset/${datasetId}/version/${datasetVersionId}/tag`,
        {
            tag,
        }
    )
    return resp.data
}

export async function deleteDatasetVersionTag(
    projectId: string,
    datasetId: string,
    datasetVersionId: string,
    tag: string
): Promise<void> {
    const resp = await axios.delete<void>(
        `/api/v1/project/${projectId}/dataset/${datasetId}/version/${datasetVersionId}/tag/${tag}`
    )
    return resp.data
}
