import { IFileSchema } from '@/domain/base/schemas/file'
import { IHasTagSchema, IResourceSchema } from '@/domain/base/schemas/resource'
import { IUserSchema } from '@user/schemas/user'

export interface IModelVersionSchema extends IResourceSchema, IHasTagSchema {
    name: string
    size: string
    owner?: IUserSchema
    stepSpecs: StepSpec[]
    builtInRuntime: string
    shared?: number
}

export interface IModelTreeVersionSchema extends IModelVersionSchema {
    versionName?: string
    createdTime?: number
}

export interface IModelVersionDetailSchema extends IResourceSchema {
    versionInfo: IModelVersionSchema
}

export interface IModelVersionDiffSchema {
    baseVersion: IFileSchema
    compareVersion: IFileSchema
}

export interface IUpdateModelVersionSchema {
    tag: string
}

export interface ICreateModelVersionSchema {
    modelName: string
    zipFile?: FileList
    importPath?: string
}

export interface RuntimeResource {
    type: string
    request: number
    limit: number
}

export interface StepSpec {
    concurrency?: number
    needs?: string[]
    resources?: RuntimeResource[]
    job_name?: string
    name?: string
    replicas?: number
    virtual?: boolean
    expose?: number
    require_dataset?: boolean
}
