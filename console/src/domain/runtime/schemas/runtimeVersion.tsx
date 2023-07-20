import { IHasTagSchema, IResourceSchema } from '@/domain/base/schemas/resource'
import { IUserSchema } from '@user/schemas/user'
import { IListQuerySchema } from '@base/schemas/list'

export interface IRuntimeVersionSchema extends IResourceSchema, IHasTagSchema {
    name: string
    owner?: IUserSchema
    image: string
    builtImage: string
    runtimeId: string
    shared?: number
}

export interface IRuntimeTreeVersionSchema extends IRuntimeVersionSchema {
    versionName?: string
    createdTime?: number
}

export interface IRuntimeVersionListSchema extends IResourceSchema {
    name: string
    versionName: string
    versionMeta: string
    versionTag: string
    versionAlias: string
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

export interface IRuntimeVersionSuggestionSchema {
    runtimes: IRuntimeVersionSchema[]
}

export interface IRuntimeVersionBuildImageResultSchema {
    success: boolean
    message: string
}
