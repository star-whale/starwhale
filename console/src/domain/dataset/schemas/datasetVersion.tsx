import { IHasTagSchema, IResourceSchema } from '@/domain/base/schemas/resource'
import { IUserSchema } from '@user/schemas/user'

export interface IDatasetFileSchema {
    name: string
    size: string
}
export interface IDatasetVersionSchema extends IResourceSchema, IHasTagSchema {
    meta: string
    owner?: IUserSchema
    shared?: number
}

export interface IDatasetTreeVersionSchema extends IDatasetVersionSchema {
    versionName?: string
    createdTime?: number
}

export interface IDatasetVersionDetailSchema {
    id?: string
    name?: string
    createdTime?: number
    versionMeta?: string
    versionName?: string
    versionTag?: string
    versionAlias?: string
    files?: Array<IDatasetFileSchema>
    indexTable?: string
    shared?: number
}

export interface IUpdateDatasetVersionSchema {
    tag?: string
}

export interface ICreateDatasetVersionSchema {
    datasetName: string
    zipFile?: FileList
    importPath?: string
}
