import { IResourceSchema } from '@/domain/base/schemas/resource'
import { IUserSchema } from '@user/schemas/user'
import { IDatasetFileSchema, IDatasetTreeVersionSchema, IDatasetVersionSchema } from './datasetVersion'

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
    versionAlias?: string
    files?: Array<IDatasetFileSchema>
    indexTable?: string
    shared?: number
}

export interface IUpdateDatasetSchema {
    description?: string
}

export interface ICreateDatasetSchema {
    datasetName: string
    zipFile?: FileList
    importPath?: string
}

export interface IDatasetTreeSchema {
    ownerName: string
    projectName: string
    datasetName: string
    shared: number
    versions: IDatasetTreeVersionSchema[]
}
