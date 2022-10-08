import { IListQuerySchema, IListSchema } from '@/domain/base/schemas/list'
import axios from 'axios'
import { IRuntimeSchema, IRuntimeDetailSchema } from '../schemas/runtime'

export async function listRuntimes(projectId: string, query: IListQuerySchema): Promise<IListSchema<IRuntimeSchema>> {
    const resp = await axios.get<IListSchema<IRuntimeSchema>>(`/api/v1/project/${projectId}/runtime`, {
        params: query,
    })
    return resp.data
}

export async function fetchRuntime(projectId: string, runtimeId: string, runtimeVersionUrl?: string): Promise<any> {
    const config = { params: runtimeVersionUrl ? { runtimeVersionUrl } : {} }
    const resp = await axios.get<IRuntimeDetailSchema>(`/api/v1/project/${projectId}/runtime/${runtimeId}`, config)
    return resp.data
}
