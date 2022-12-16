import axios from 'axios'
import { IListQuerySchema, IListSchema } from '@/domain/base/schemas/list'
import {
    ICreateModelVersionSchema,
    IModelVersionSchema,
    IUpdateModelVersionSchema,
    IModelVersionDetailSchema,
} from '../schemas/modelVersion'

export async function listModelVersions(
    projectId: string,
    modelId: string,
    query: IListQuerySchema
): Promise<IListSchema<IModelVersionSchema>> {
    const resp = await axios.get<IListSchema<IModelVersionSchema>>(
        `/api/v1/project/${projectId}/model/${modelId}/version`,
        {
            params: query,
        }
    )
    return resp.data
}

export async function fetchModelVersion(projectId: string, modelId: string, modelVersionId: string): Promise<any> {
    const resp = await axios.get<IModelVersionDetailSchema>(
        `/api/v1/project/${projectId}/model/${modelId}?versionUrl=${modelVersionId}`
    )
    return resp.data
}
export async function createModelVersion(
    projectId: string,
    modelId: string,
    data: ICreateModelVersionSchema
): Promise<IModelVersionSchema> {
    const bodyFormData = new FormData()
    bodyFormData.append('importPath', data.importPath ?? '')
    if (data.zipFile && data.zipFile.length > 0) bodyFormData.append('zipFile', data.zipFile[0] as File)

    const resp = await axios({
        method: 'post',
        url: `/api/v1/project/${projectId}/model/${modelId}/version`,
        data: bodyFormData,
        headers: { 'Content-Type': 'multipart/form-data' },
    })
    return resp.data
}

export async function updateModelVersion(
    projectId: string,
    modelId: string,
    modelVersionId: string,
    data: IUpdateModelVersionSchema
): Promise<IModelVersionSchema> {
    const resp = await axios.patch<IModelVersionSchema>(
        `/api/v1/project/${projectId}/model/${modelId}/version/${modelVersionId}`,
        data
    )
    return resp.data
}

export async function revertModelVersion(
    projectId: string,
    modelId: string,
    modelVersionId: string
): Promise<IModelVersionSchema> {
    const resp = await axios.post<IModelVersionSchema>(`/api/v1/project/${projectId}/model/${modelId}/revert`, {
        versionUrl: modelVersionId,
    })
    return resp.data
}
