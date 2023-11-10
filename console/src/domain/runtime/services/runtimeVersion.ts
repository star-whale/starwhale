import axios from 'axios'
import qs from 'qs'
import { IRuntimeVersionBuildImageResultSchema, IRuntimeVersionListQuerySchema } from '../schemas/runtimeVersion'
import { IPageInfoRuntimeVersionVo, IRuntimeInfoVo, IRuntimeSuggestionVo } from '@/api'

export async function listRuntimeVersions(
    projectId: string,
    runtimeId: string,
    query: IRuntimeVersionListQuerySchema
): Promise<IPageInfoRuntimeVersionVo> {
    const resp = await axios.get<IPageInfoRuntimeVersionVo>(
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
): Promise<IRuntimeInfoVo> {
    const resp = await axios.get<IRuntimeInfoVo>(
        `/api/v1/project/${projectId}/runtime/${runtimeId}?${qs.stringify({
            versionUrl: runtimeVersionId,
        })}`
    )
    return resp.data
}

export async function revertRuntimeVersion(
    projectId: string,
    runtimeId: string,
    runtimeVersionId: string
): Promise<string> {
    const resp = await axios.post<string>(`/api/v1/project/${projectId}/runtime/${runtimeId}/revert`, {
        versionUrl: runtimeVersionId,
    })
    return resp.data
}

export async function fetchRuntimeVersionSuggestion(
    projectId: string,
    modelVersionId: string
): Promise<IRuntimeSuggestionVo> {
    const { data } = await axios.get<IRuntimeSuggestionVo>('/api/v1/job/suggestion/runtime', {
        params: { projectId, modelVersionId },
    })
    return data
}

export async function updateRuntimeVersionShared(
    projectId: string,
    runtimeId: string,
    runtimeVersionId: string,
    shared: boolean
): Promise<string> {
    const resp = await axios.put<string>(
        `/api/v1/project/${projectId}/runtime/${runtimeId}/version/${runtimeVersionId}/shared?shared=${shared ? 1 : 0}`
    )
    return resp.data
}

export async function buildImageForRuntimeVersion(
    projectId: string,
    runtimeId: string,
    runtimeVersionId: string
): Promise<IRuntimeVersionBuildImageResultSchema> {
    const resp = await axios.post<IRuntimeVersionBuildImageResultSchema>(
        `/api/v1/project/${projectId}/runtime/${runtimeId}/version/${runtimeVersionId}/image/build`
    )
    return resp.data
}

export async function addRuntimeVersionTag(
    projectId: string,
    runtimeId: string,
    runtimeVersionId: string,
    tag: string
): Promise<void> {
    const resp = await axios.post<void>(
        `/api/v1/project/${projectId}/runtime/${runtimeId}/version/${runtimeVersionId}/tag`,
        {
            tag,
        }
    )
    return resp.data
}

export async function deleteRuntimeVersionTag(
    projectId: string,
    runtimeId: string,
    runtimeVersionId: string,
    tag: string
): Promise<void> {
    const resp = await axios.delete<void>(
        `/api/v1/project/${projectId}/runtime/${runtimeId}/version/${runtimeVersionId}/tag/${tag}`
    )
    return resp.data
}
