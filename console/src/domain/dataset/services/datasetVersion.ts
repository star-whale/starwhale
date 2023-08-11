import axios from 'axios'
import { IListQuerySchema, IListSchema } from '@/domain/base/schemas/list'
import {
    ICreateDatasetVersionSchema,
    IDatasetVersionSchema,
    IUpdateDatasetVersionSchema,
    IDatasetVersionDetailSchema,
} from '../schemas/datasetVersion'
import { IDatasetSchema } from '../schemas/dataset'

export async function listDatasetVersions(
    projectId: string,
    datasetId: string,
    query: IListQuerySchema
): Promise<IListSchema<IDatasetVersionSchema>> {
    const resp = await axios.get<IListSchema<IDatasetVersionSchema>>(
        `/api/v1/project/${projectId}/dataset/${datasetId}/version`,
        {
            params: query,
        }
    )
    return resp.data
}

export async function listDatasetVersionsByIds(
    projectId: string,
    datasetVersionIds: string,
    query: IListQuerySchema
): Promise<IListSchema<IDatasetSchema>> {
    const resp = await axios.get<IListSchema<IDatasetSchema>>(`/api/v1/project/${projectId}/dataset`, {
        params: {
            ...query,
            versionId: datasetVersionIds,
        },
    })
    return resp.data
}

export async function fetchDatasetVersion(
    projectId: string,
    datasetId: string,
    datasetVersionId: string
): Promise<any> {
    const resp = await axios.get<IDatasetVersionDetailSchema>(
        `/api/v1/project/${projectId}/dataset/${datasetId}?versionUrl=${datasetVersionId}`
    )
    return resp.data
}

export async function createDatasetVersion(
    projectId: string,
    datasetId: string,
    data: ICreateDatasetVersionSchema
): Promise<IDatasetVersionSchema> {
    const bodyFormData = new FormData()
    bodyFormData.append('importPath', data.importPath ?? '')
    if (data.zipFile && data.zipFile.length > 0) bodyFormData.append('zipFile', data.zipFile[0] as File)

    const resp = await axios({
        method: 'post',
        url: `/api/v1/project/${projectId}/dataset/${datasetId}/version`,
        data: bodyFormData,
        headers: { 'Content-Type': 'multipart/form-data' },
    })
    return resp.data
}

export async function updateDatasetVersion(
    projectId: string,
    datasetId: string,
    datasetVersionId: string,
    data: IUpdateDatasetVersionSchema
): Promise<IDatasetVersionSchema> {
    const resp = await axios.patch<IDatasetVersionSchema>(
        `/api/v1/project/${projectId}/dataset/${datasetId}/version/${datasetVersionId}`,
        data
    )
    return resp.data
}

export async function revertDatasetVersion(
    projectId: string,
    datasetId: string,
    datasetVersionId: string
): Promise<IDatasetVersionSchema> {
    const resp = await axios.post<IDatasetVersionSchema>(`/api/v1/project/${projectId}/dataset/${datasetId}/revert`, {
        versionUrl: datasetVersionId,
    })
    return resp.data
}

export async function updateDatasetVersionShared(
    projectId: string,
    datasetId: string,
    datasetVersionId: string,
    shared: boolean
): Promise<IDatasetVersionSchema> {
    const resp = await axios.put<IDatasetVersionSchema>(
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
