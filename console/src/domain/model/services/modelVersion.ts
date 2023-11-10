import axios from 'axios'
import { IListQuerySchema } from '@/domain/base/schemas/list'
import { IModelVersionDiffSchema } from '../schemas/modelVersion'
import { IFileSchema } from '@/domain/base/schemas/file'
import { IModelInfoVo, IPageInfoModelVersionVo } from '@/api'

export async function listModelVersions(
    projectId: string,
    modelId: string,
    query: IListQuerySchema
): Promise<IPageInfoModelVersionVo> {
    const resp = await axios.get<IPageInfoModelVersionVo>(`/api/v1/project/${projectId}/model/${modelId}/version`, {
        params: query,
    })
    return resp.data
}

export async function listModelVersionFiles(
    projectId: string,
    modelId: string,
    query: { version?: string; path?: string }
): Promise<IFileSchema> {
    const resp = await axios.get<IFileSchema>(`/api/v1/project/${projectId}/model/${modelId}/listFiles`, {
        params: query,
    })
    return resp.data
}

export async function fetchModelVersion(
    projectId: string,
    modelId: string,
    modelVersionId: string
): Promise<IModelInfoVo> {
    const resp = await axios.get<IModelInfoVo>(
        `/api/v1/project/${projectId}/model/${modelId}?versionUrl=${modelVersionId}`
    )
    return resp.data
}

export async function fetchModelVersionDiff(
    projectId: string,
    modelId: string,
    modelVersionId: string,
    compareVersion: string
): Promise<IModelVersionDiffSchema> {
    const resp = await axios.get<IModelVersionDiffSchema>(
        `/api/v1/project/${projectId}/model/${modelId}/diff?baseVersion=${modelVersionId}&compareVersion=${compareVersion}`
    )
    return resp.data
}

export async function revertModelVersion(projectId: string, modelId: string, modelVersionId: string): Promise<string> {
    const resp = await axios.post<string>(`/api/v1/project/${projectId}/model/${modelId}/revert`, {
        versionUrl: modelVersionId,
    })
    return resp.data
}

export async function fetchModelVersionFile(
    projectName: string | undefined,
    modelName: string | undefined,
    modelVersionId: string | undefined,
    token: string,
    path: string
): Promise<any> {
    if (!projectName || !modelName) {
        return Promise.resolve(undefined)
    }

    const { data } = await axios.get(`/api/v1/project/${projectName}/model/${modelName}/getFileData`, {
        params: { Authorization: token, path, version: modelVersionId, silent: true },
        transformResponse: (res) => {
            return res
        },
    })
    return data
}

export async function fetchModelVersionPanelSetting(
    projectName: string | undefined,
    modelName: string | undefined,
    modelVersionId: string | undefined,
    token: string
) {
    return fetchModelVersionFile(projectName, modelName, modelVersionId, token, 'eval_panel_layout.json').then((raw) =>
        raw ? JSON.parse(raw) : undefined
    )
}

export async function updateModelVersionShared(
    projectId: string,
    modelId: string,
    modelVersionId: string,
    shared: boolean
): Promise<string> {
    const resp = await axios.put<string>(
        `/api/v1/project/${projectId}/model/${modelId}/version/${modelVersionId}/shared?shared=${shared ? 1 : 0}`
    )
    return resp.data
}

export async function addModelVersionTag(
    projectId: string,
    modelId: string,
    modelVersionId: string,
    tag: string
): Promise<void> {
    const resp = await axios.post<void>(`/api/v1/project/${projectId}/model/${modelId}/version/${modelVersionId}/tag`, {
        tag,
    })
    return resp.data
}

export async function deleteModelVersionTag(
    projectId: string,
    modelId: string,
    modelVersionId: string,
    tag: string
): Promise<void> {
    const resp = await axios.delete<void>(
        `/api/v1/project/${projectId}/model/${modelId}/version/${modelVersionId}/tag/${tag}`
    )
    return resp.data
}
