import axios from 'axios'
import { IListQuerySchema } from '@/domain/base/schemas/list'
import { IPageInfoTrashVo } from '@/api'

export async function listTrashes(projectId: string, query: IListQuerySchema): Promise<IPageInfoTrashVo> {
    const resp = await axios.get<IPageInfoTrashVo>(`/api/v1/project/${projectId}/trash`, { params: query })
    return resp.data
}

export async function removeTrash(projectId: string, trashId: string): Promise<any> {
    const resp = await axios.delete(`/api/v1/project/${projectId}/trash/${trashId}`)
    return resp.data
}

export async function recoverTrash(projectId: string, trashId: string): Promise<any> {
    const resp = await axios.put(`/api/v1/project/${projectId}/trash/${trashId}`)
    return resp.data
}
