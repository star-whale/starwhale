import axios from 'axios'
import { IListQuerySchema, IListSchema } from '@/domain/base/schemas/list'
import { IUserRoleSchema } from '@user/schemas/user'
import { ICreateProjectSchema, IUpdateProjectSchema, IProjectSchema, IProjectRoleSchema } from '../schemas/project'

export async function listProjects(query: IListQuerySchema & { sort?: string }): Promise<IListSchema<IProjectSchema>> {
    const resp = await axios.get<IListSchema<IProjectSchema>>('/api/v1/project', { params: query })
    return resp.data
}

export async function fetchProject(projectId: string): Promise<any> {
    const resp = await axios.get<IProjectSchema>(`/api/v1/project/${projectId}`)
    return resp.data
}

export async function fetchProjectReadme(projectId: string): Promise<string> {
    const resp = await axios.get<string>(`/api/v1/project/${projectId}/readme`)
    return resp.data
}

export async function createProject(data: ICreateProjectSchema): Promise<IProjectSchema> {
    const resp = await axios.post<IProjectSchema>('/api/v1/project', data)
    return resp.data
}

export async function removeProject(projectId: string): Promise<string> {
    const { data } = await axios.delete<string>(`/api/v1/project/${projectId}`)
    return data
}

export async function changeProject(projectId: string, data: IUpdateProjectSchema): Promise<IProjectSchema> {
    const resp = await axios.put<IProjectSchema>(`/api/v1/project/${projectId}`, data)
    return resp.data
}

export async function listRoles(): Promise<IUserRoleSchema[]> {
    const { data } = await axios.get<IUserRoleSchema[]>('/api/v1/role/enums')
    return data
}

function projectRoleUrl(projectId: string, projectRoleId?: string) {
    return [`/api/v1/project/${projectId}/role`, projectRoleId].filter((i) => !!i).join('/')
}

export async function listProjectRole(projectId: string): Promise<IProjectRoleSchema[]> {
    const { data } = await axios.get<IProjectRoleSchema[]>(projectRoleUrl(projectId))
    return data
}

export async function addProjectRole(projectId: string, userId: string, roleId: string): Promise<string> {
    const params = { params: { userId, roleId } }
    const { data } = await axios.post<string>(projectRoleUrl(projectId), null, params)
    return data
}

export async function removeProjectRole(projectId: string, projectRoleId: string): Promise<string> {
    const { data } = await axios.delete<string>(projectRoleUrl(projectId, projectRoleId))
    return data
}

export async function changeProjectRole(projectId: string, projectRoleId: string, roleId: string): Promise<string> {
    const params = { params: { roleId } }
    const { data } = await axios.put<string>(projectRoleUrl(projectId, projectRoleId), null, params)
    return data
}
