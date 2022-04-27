import { IResourceSchema } from '@/domain/base/schemas/resource'
import { IUserSchema } from '@user/schemas/user'

export interface IDatasetVersionSchema extends IResourceSchema {
    name: string
    tag: string
    createTime: number
    meta: string
    owner?: IUserSchema
}

export interface IDatasetVersionDetailSchema extends IDatasetVersionSchema {
    datasetName?: string
}

export interface IUpdateDatasetVersionSchema {
    tag: string
}

export interface ICreateDatasetVersionSchema {
    datasetName: string
    zipFile?: FileList
    importPath?: string
}
