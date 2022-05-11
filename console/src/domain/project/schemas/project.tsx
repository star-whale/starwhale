import { IResourceSchema } from '@/domain/base/schemas/resource'
import { IUserSchema } from '@user/schemas/user'

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
