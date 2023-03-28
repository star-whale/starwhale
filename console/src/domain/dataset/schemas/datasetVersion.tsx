import { IResourceSchema } from '@/domain/base/schemas/resource'
import { IUserSchema } from '@user/schemas/user'

export interface IDatasetFileSchema {
    name: string
    size: string
}
export interface IDatasetVersionSchema extends IResourceSchema {
    name: string
    tag: string
    meta: string
    owner?: IUserSchema
    alias: string
    shared?: number
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
