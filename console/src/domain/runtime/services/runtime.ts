import { IListQuerySchema, IListSchema } from '@/domain/base/schemas/list'
import axios from 'axios'
import { IRuntimeSchema, IRuntimeDetailSchema, IRuntimeTreeSchema } from '../schemas/runtime'

export async function listRuntimes(
    projectId: string,
    query: IListQuerySchema & { name?: string }
): Promise<IListSchema<IRuntimeSchema>> {
    const resp = await axios.get<IListSchema<IRuntimeSchema>>(`/api/v1/project/${projectId}/runtime`, {
        params: query,
    })
    return resp.data
}

export async function fetchRuntime(
    projectId: string,
    runtimeId: string,
    versionUrl?: string
): Promise<IRuntimeDetailSchema> {
    const config = { params: versionUrl ? { versionUrl } : {} }
    const resp = await axios.get<IRuntimeDetailSchema>(`/api/v1/project/${projectId}/runtime/${runtimeId}`, config)
    return resp.data
}

export async function removeRuntime(projectId: string, runtimeId: string): Promise<any> {
    const resp = await axios.delete<IRuntimeDetailSchema>(`/api/v1/project/${projectId}/runtime/${runtimeId}`)
    return resp.data
}

export async function fetchRuntimeTree(projectId: string): Promise<IRuntimeTreeSchema[]> {
    const resp = await axios.get<IRuntimeTreeSchema[]>(`/api/v1/project/${projectId}/runtime-tree`)
    return resp.data
}

export async function fetchRecentRuntimeTree(projectId: string): Promise<IRuntimeTreeSchema[]> {
    const resp = await axios.get<IRuntimeTreeSchema[]>(`/api/v1/project/${projectId}/recent-runtime-tree`)
    return resp.data
}
