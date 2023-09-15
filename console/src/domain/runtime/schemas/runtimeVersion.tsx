import { IHasTagSchema, IResourceSchema } from '@/domain/base/schemas/resource'
import { IUserSchema } from '@user/schemas/user'
import { IListQuerySchema } from '@base/schemas/list'

export interface IRuntimeVersionSchema extends IResourceSchema, IHasTagSchema {
    name: string
    owner?: IUserSchema
    image: string
    builtImage: string
    runtimeId: string
    shared?: boolean
    meta: string
}

export interface IRuntimeTreeVersionSchema extends IRuntimeVersionSchema {
    versionName?: string
    createdTime?: number
}

export interface IRuntimeVersionDetailSchema extends IRuntimeVersionSchema {
    modelName?: string
}

export interface IUpdateRuntimeVersionSchema {
    tag: string
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
