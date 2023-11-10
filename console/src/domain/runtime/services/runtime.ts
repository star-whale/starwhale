import { IListQuerySchema } from '@/domain/base/schemas/list'
import axios from 'axios'
import { IPageInfoRuntimeVo, IRuntimeInfoVo, IRuntimeViewVo } from '@/api'

export async function listRuntimes(
    projectId: string,
    query: IListQuerySchema & { name?: string }
): Promise<IPageInfoRuntimeVo> {
    const resp = await axios.get<IPageInfoRuntimeVo>(`/api/v1/project/${projectId}/runtime`, {
        params: query,
    })
    return resp.data
}

export async function fetchRuntime(projectId: string, runtimeId: string, versionUrl?: string): Promise<IRuntimeInfoVo> {
    const config = { params: versionUrl ? { versionUrl } : {} }
    const resp = await axios.get<IRuntimeInfoVo>(`/api/v1/project/${projectId}/runtime/${runtimeId}`, config)
    return resp.data
}

export async function removeRuntime(projectId: string, runtimeId: string): Promise<string> {
    const resp = await axios.delete<string>(`/api/v1/project/${projectId}/runtime/${runtimeId}`)
    return resp.data
}

export async function fetchRuntimeTree(projectId: string): Promise<IRuntimeViewVo[]> {
    const resp = await axios.get<IRuntimeViewVo[]>(`/api/v1/project/${projectId}/runtime-tree`)
    return resp.data
}

export async function fetchRecentRuntimeTree(projectId: string): Promise<IRuntimeViewVo[]> {
    const resp = await axios.get<IRuntimeViewVo[]>(`/api/v1/project/${projectId}/recent-runtime-tree`)
    return resp.data
}
