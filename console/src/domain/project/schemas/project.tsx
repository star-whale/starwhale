import { IResourceSchema } from '@/domain/base/schemas/resource'
import { IUserSchema, IUserRoleSchema } from '@user/schemas/user'

export interface IProjectSchema extends IResourceSchema {
    name: string
    owner?: IUserSchema
}

export interface IUpdateProjectSchema {
    description?: string
}

export interface ICreateProjectSchema {
    projectName: string
    description?: string
}

export interface IProjectRoleSchema {
    id: string
    user: IUserSchema
    project: IProjectRoleSchema
    role: IUserRoleSchema
}
