import axios from 'axios'
import { IListQuerySchema, IListSchema } from '@/domain/base/schemas/list'
import { ITrashSchema } from '../schemas/trash'

export async function listTrashs(projectId: string, query: IListQuerySchema): Promise<IListSchema<ITrashSchema>> {
    const resp = await axios.get<IListSchema<ITrashSchema>>(`/api/v1/project/${projectId}/trash`, { params: query })
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
