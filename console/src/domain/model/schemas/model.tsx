import { IResourceSchema } from '@/domain/base/schemas/resource'
import { IUserSchema } from '@user/schemas/user'
import { IModelTreeVersionSchema, IModelVersionSchema } from './modelVersion'

export interface IModelSchema extends IResourceSchema {
    name: string
    owner?: IUserSchema
    version?: IModelVersionSchema
}

export interface IModelDetailSchema {
    id?: string
    name?: string
    versionName?: string
    versionTag?: string
    versionAlias?: string
    createdTime?: number
    files?: Array<IModelFileSchema>
    shared?: number
}

export interface IModelFileSchema {
    name: string
    size: string
}

export interface IUpdateModelSchema {
    description?: string
}

export interface ICreateModelSchema {
    modelName: string
    zipFile?: FileList
    importPath?: string
}

export interface ICreateOnlineEvalSchema {
    modelVersionUrl: string
    runtimeVersionUrl: string
    resourcePool: string
    spec: string
}

export interface IModelTreeSchema {
    modelId: string
    ownerName: string
    projectName: string
    modelName: string
    shared: number
    versions: IModelTreeVersionSchema[]
}
