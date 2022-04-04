import axios from 'axios'
import { ICreateProjectSchema, IProjectSchema, IUpdateProjectSchema } from '../schemas/project'
import { IListQuerySchema, IListSchema } from '@/schemas/list'
import { IEventSchema } from '@/schemas/event'
import { ResourceType } from '@/schemas/resource'

export async function listProjects(query: IListQuerySchema): Promise<IListSchema<IProjectSchema>> {
    const resp = await axios.get<IListSchema<IProjectSchema>>('/api/v1/project', { params: query })
    return resp.data
}

export async function fetchProject(projectId: string): Promise<any> {
    const resp = await axios.get<IProjectSchema>(`/api/v1/project/${projectId}`)
    return resp.data
}

export async function createProject(data: ICreateProjectSchema): Promise<IProjectSchema> {
    const resp = await axios.post<IProjectSchema>('/api/v1/project', data)
    return resp.data
}
