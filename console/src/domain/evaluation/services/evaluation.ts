import axios from 'axios'
import { IListQuerySchema } from '@/domain/base/schemas/list'
import { IEvaluationAttributeValue, IEvaluationViewSchema } from '../schemas/evaluation'

export async function listEvaluationAttrs(
    projectId: string,
    query: IListQuerySchema
): Promise<IEvaluationAttributeValue[]> {
    const resp = await axios.get<IEvaluationAttributeValue[]>(
        `/api/v1/project/${projectId}/evaluation/view/attribute`,
        {
            params: query,
        }
    )
    return resp.data
}

export async function getEvaluationViewConfig(projectId: string, name = 'evaluation'): Promise<any> {
    const resp = await axios.get<IEvaluationViewSchema>(`/api/v1/project/${projectId}/evaluation/view/config`, {
        params: {
            name,
        },
    })
    return resp.data
}

export async function setEvaluationViewConfig(projectId: string, data: any): Promise<IEvaluationViewSchema> {
    const resp = await axios.post<IEvaluationViewSchema>(`/api/v1/project/${projectId}/evaluation/view/config`, data)

    return resp.data
}
