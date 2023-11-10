import axios from 'axios'
import { IListQuerySchema } from '@/domain/base/schemas/list'
import { IModelInfoVo, IModelViewVo, IPageInfoModelVo } from '@/api'

export async function listModels(
    projectId: string,
    query: IListQuerySchema & { name?: string }
): Promise<IPageInfoModelVo> {
    const resp = await axios.get<IPageInfoModelVo>(`/api/v1/project/${projectId}/model`, {
        params: query,
    })
    return resp.data
}

export async function fetchModel(projectId: string, modelId: string): Promise<IModelInfoVo> {
    const resp = await axios.get<IModelInfoVo>(`/api/v1/project/${projectId}/model/${modelId}`)
    return resp.data
}

export async function removeModel(projectId: string, modelId: string): Promise<any> {
    const resp = await axios.delete(`/api/v1/project/${projectId}/model/${modelId}`)
    return resp.data
}

export async function fetchModelTree(projectId: string): Promise<IModelViewVo[]> {
    const resp = await axios.get<IModelViewVo[]>(`/api/v1/project/${projectId}/model-tree`)
    return resp.data
}

export async function fetchRecentModelTree(projectId: string): Promise<IModelViewVo[]> {
    const resp = await axios.get<IModelViewVo[]>(`/api/v1/project/${projectId}/recent-model-tree?slient=true`)
    return resp.data
}
