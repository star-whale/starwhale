import { IResourceSchema } from '@/domain/base/schemas/resource'
import { IUserSchema } from '@user/schemas/user'
import { IListQuerySchema } from '../../base/schemas/list'

export interface IRuntimeVersionSchema extends IResourceSchema {
    name: string
    tag: string
    meta: Record<string, unknown>
    owner?: IUserSchema
}

export interface IRuntimeVersionListSchema extends IResourceSchema {
    name: string
    versionName: string
    versionMeta: string
    versionTag: string
    manifest: string
}

export interface IRuntimeVersionDetailSchema extends IRuntimeVersionSchema {
    modelName?: string
}

export interface IUpdateRuntimeVersionSchema {
    tag: string
}

export interface ICreateRuntimeVersionSchema {
    file?: File
}

export interface IRuntimeVersionListQuerySchema extends IListQuerySchema {
    vName?: string
    vTag?: string
}
