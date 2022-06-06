import axios from 'axios'
import { IListSchema } from '@/domain/base/schemas/list'
import qs from 'qs'
import {
    IRuntimeVersionSchema,
    IUpdateRuntimeVersionSchema,
    IRuntimeVersionDetailSchema,
    IRuntimeVersionListQuerySchema,
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
    const resp = await axios.head<IRuntimeVersionDetailSchema>(
        `/api/v1/project/${projectId}/runtime/${runtimeId}/version/${runtimeVersionId}?queryRequest=${qs.stringify({
            runtime: runtimeId,
            project: projectId,
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
    const resp = await axios.patch<IRuntimeVersionSchema>(
        `/api/v1/project/${projectId}/runtime/${runtimeId}/version/${runtimeVersionId}`,
        data
    )
    return resp.data
}

export async function revertRuntimeVersion(
    projectId: string,
    runtimeId: string,
    runtimeVersionId: string
): Promise<IRuntimeVersionSchema> {
    const resp = await axios.post<IRuntimeVersionSchema>(
        `/api/v1/project/${projectId}/runtime/${runtimeId}/version/${runtimeVersionId}/revert`,
        {
            versonId: runtimeVersionId,
        }
    )
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
