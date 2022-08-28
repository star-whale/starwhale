import { IResourceSchema } from '@/domain/base/schemas/resource'
import { IUserSchema } from '@user/schemas/user'
import { IDatasetVersionSchema } from './datasetVersion'

export interface IDatasetSchema extends IResourceSchema {
    name: string
    owner?: IUserSchema
    version?: IDatasetVersionSchema
}

export interface IDatasetDetailSchema {
    id?: string
    name?: string
    createdTime?: number
    versionMeta?: string
    versionName?: string
    versionTag?: string
    files?: Array<IDatasetFileSchema>
    indexTable?: string
}

export interface IDatasetFileSchema {
    name: string
    size: string
}

export interface IUpdateDatasetSchema {
    description?: string
}

export interface ICreateDatasetSchema {
    datasetName: string
    zipFile?: FileList
    importPath?: string
}
