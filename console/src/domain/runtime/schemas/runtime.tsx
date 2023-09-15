import { IResourceSchema } from '@/domain/base/schemas/resource'
import { IUserSchema } from '@user/schemas/user'
import { IRuntimeTreeVersionSchema, IRuntimeVersionSchema } from './runtimeVersion'

export interface IRuntimeSchema extends IResourceSchema {
    name: string
    owner: IUserSchema
    version: IRuntimeVersionSchema
}

export interface IRuntimeDetailSchema {
    id?: string
    name?: string
    files?: Array<IRuntimeFileSchema>
    versionInfo: IRuntimeVersionSchema
}

export interface IRuntimeFileSchema {
    name: string
    size: string
}

export interface IRuntimeTreeSchema {
    ownerName: string
    projectName: string
    runtimeName: string
    shared: boolean
    versions: IRuntimeTreeVersionSchema[]
}
