import { IResourceSchema } from '@/domain/base/schemas/resource'
import { IUserSchema } from '../../user/schemas/user'
import { IRuntimeTreeVersionSchema, IRuntimeVersionSchema } from './runtimeVersion'

export interface IRuntimeSchema extends IResourceSchema {
    name: string
    owner: IUserSchema
    version: IRuntimeVersionSchema
}

export interface IRuntimeDetailSchema {
    name?: string
    versionMeta?: string
    versionName?: string
    versionTag?: string
    versionAlias?: string
    createdTime?: number
    files?: Array<IRuntimeFileSchema>
    id?: string
    shared?: number
}

export interface IRuntimeFileSchema {
    name: string
    size: string
}

export interface IUpdateRuntimeSchema {
    description?: string
}

export interface ICreateRuntimeSchema {
    modelName: string
    zipFile?: FileList
}

export interface IRuntimeTreeSchema {
    ownerName: string
    projectName: string
    runtimeName: string
    shared: number
    versions: IRuntimeTreeVersionSchema[]
}
