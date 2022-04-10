import axios from 'axios'
import { ICreateModelSchema, IModelSchema, IUpdateModelSchema, IModelDetailSchema } from '../schemas/model'
import { IListQuerySchema, IListSchema } from '@/domain/base/schemas/list'

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
    var bodyFormData = new FormData()
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
