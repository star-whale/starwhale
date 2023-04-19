import { IFileSchema } from '@/domain/base/schemas/file'
import { IResourceSchema } from '@/domain/base/schemas/resource'
import { IUserSchema } from '@user/schemas/user'

export interface IModelVersionSchema extends IResourceSchema {
    name: string
    tag: string
    alias: string
    meta: Record<string, unknown>
    owner?: IUserSchema
    stepSpecs: StepSpec[]
}

export interface IModelTreeVersionSchema extends IModelVersionSchema {
    versionName?: string
    createdTime?: number
}

export interface IModelVersionListSchema extends IResourceSchema, IFileSchema {
    name: string
    versionName: string
    versionMeta: string
    versionTag: string
    versionAlias: string
    manifest: string
    createdTime?: number
}

// eslint-disable-next-line @typescript-eslint/no-empty-interface
export interface IModelVersionDetailSchema extends IModelVersionListSchema {}

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
}
