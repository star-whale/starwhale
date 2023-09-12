import axios from 'axios'
import { IListQuerySchema, IListSchema } from '@/domain/base/schemas/list'
import {
    ICreateModelSchema,
    IModelSchema,
    IModelDetailSchema,
    ICreateOnlineEvalSchema,
    IModelTreeSchema,
} from '../schemas/model'

export async function listModels(projectId: string, query: IListQuerySchema): Promise<IListSchema<IModelSchema>> {
    const resp = await axios.get<IListSchema<IModelSchema>>(`/api/v1/project/${projectId}/model`, {
        params: query,
    })
    return resp.data
}

export async function fetchModel(projectId: string, modelId: string): Promise<any> {
    const resp = await axios.get<IModelDetailSchema>(`/api/v1/project/${projectId}/model/${modelId}`)
    return resp.data
}

export async function createModel(projectId: string, data: ICreateModelSchema): Promise<IModelSchema> {
    const bodyFormData = new FormData()
    bodyFormData.append('modelName', data.modelName)
    bodyFormData.append('importPath', data.importPath ?? '')
    if (data.zipFile && data.zipFile.length > 0) bodyFormData.append('zipFile', data.zipFile[0] as File)

    const resp = await axios({
        method: 'post',
        url: `/api/v1/project/${projectId}/model`,
        data: bodyFormData,
        headers: { 'Content-Type': 'multipart/form-data' },
    })
    return resp.data
}

export async function removeModel(projectId: string, modelId: string): Promise<any> {
    const resp = await axios.delete(`/api/v1/project/${projectId}/model/${modelId}`)
    return resp.data
}

export async function createOnlineEval(projectId: string, data: ICreateOnlineEvalSchema): Promise<any> {
    const resp = await axios.post<any>(`/api/v1/project/${projectId}/serving`, data)
    return resp.data
}

export async function fetchModelTree(projectId: string): Promise<IModelTreeSchema[]> {
    const resp = await axios.get<IModelTreeSchema[]>(`/api/v1/project/${projectId}/model-tree`)
    return resp.data
}

export async function fetchRecentModelTree(projectId: string): Promise<IModelTreeSchema[]> {
    const resp = await axios.get<IModelTreeSchema[]>(`/api/v1/project/${projectId}/recent-model-tree`)
    return resp.data
}
