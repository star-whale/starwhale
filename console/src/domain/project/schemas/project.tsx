import { IResourceSchema } from '@/domain/base/schemas/resource'
import { IUserSchema, IUserRoleSchema } from '@user/schemas/user'

export interface IProjectSchema extends IResourceSchema {
    name: string
    owner?: IUserSchema
    privacy?: string
    description?: string
    statistics: {
        modelCounts: number
        datasetCounts: number
        evaluationCounts: number
        memberCounts: number
        runtimeCounts: number
    }
}

export interface IUpdateProjectSchema {
    description?: string
}

export interface ICreateProjectSchema {
    ownerId?: string
    projectName: string
    privacy?: string
    description?: string
}

export interface IProjectRoleSchema {
    id: string
    user: IUserSchema
    project: IProjectSchema
    role: IUserRoleSchema
}
