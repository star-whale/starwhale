import axios from 'axios'
import { IListSchema } from '@/domain/base/schemas/list'
import qs from 'qs'
import {
    IRuntimeVersionSchema,
    IUpdateRuntimeVersionSchema,
    IRuntimeVersionDetailSchema,
    IRuntimeVersionListQuerySchema,
    IRuntimeVersionSuggestionSchema,
} from '../schemas/runtimeVersion'

export async function listRuntimeVersions(
    projectId: string,
    runtimeId: string,
    query: IRuntimeVersionListQuerySchema
): Promise<IListSchema<IRuntimeVersionSchema>> {
    const resp = await axios.get<IListSchema<IRuntimeVersionSchema>>(
        `/api/v1/project/${projectId}/runtime/${runtimeId}/version`,
        {
            params: query,
        }
    )
    return resp.data
}

export async function fetchRuntimeVersion(
    projectId: string,
    runtimeId: string,
    runtimeVersionId: string
): Promise<any> {
    const resp = await axios.get<IRuntimeVersionDetailSchema>(
        `/api/v1/project/${projectId}/runtime/${runtimeId}?${qs.stringify({
            versionUrl: runtimeVersionId,
        })}`
    )
    return resp.data
}

export async function updateRuntimeVersion(
    projectId: string,
    runtimeId: string,
    runtimeVersionId: string,
    data: IUpdateRuntimeVersionSchema
): Promise<IRuntimeVersionSchema> {
    const resp = await axios.put<IRuntimeVersionSchema>(
        `/api/v1/project/${projectId}/runtime/${runtimeId}/version/${runtimeVersionId}?tag=${data?.tag}`,
        data
    )
    return resp.data
}

export async function revertRuntimeVersion(
    projectId: string,
    runtimeId: string,
    runtimeVersionId: string
): Promise<IRuntimeVersionSchema> {
    const resp = await axios.post<IRuntimeVersionSchema>(`/api/v1/project/${projectId}/runtime/${runtimeId}/revert`, {
        versionUrl: runtimeVersionId,
    })
    return resp.data
}

export async function recoverRuntimeVersion(
    projectId: string,
    runtimeId: string,
    runtimeVersionId: string
): Promise<IRuntimeVersionSchema> {
    const resp = await axios.patch<IRuntimeVersionSchema>(
        `/api/v1/project/${projectId}/runtime/${runtimeId}/version/${runtimeVersionId}/recover`
    )
    return resp.data
}

export async function fetchRuntimeVersionSuggestion(
    projectId: string,
    modelVersionId: string
): Promise<IRuntimeVersionSuggestionSchema> {
    const { data } = await axios.get<IRuntimeVersionSuggestionSchema>('/api/v1/job/suggestion/runtime', {
        params: { projectId, modelVersionId },
    })
    return data
}

export async function updateRuntimeVersionShared(
    projectId: string,
    runtimeId: string,
    runtimeVersionId: string,
    shared: boolean
): Promise<IRuntimeVersionSchema> {
    const resp = await axios.put<IRuntimeVersionSchema>(
        `/api/v1/project/${projectId}/runtime/${runtimeId}/version/${runtimeVersionId}/shared?shared=${shared ? 1 : 0}`
    )
    return resp.data
}
