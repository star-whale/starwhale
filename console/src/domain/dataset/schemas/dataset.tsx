import { IResourceSchema } from '@/schemas/resource'
import { IUserSchema } from '@user/schemas/user'
// todo fix dataset & dataset detail has different attrs
export interface IDatasetSchema extends IResourceSchema {
    name: string
    createTime: number
    owner?: IUserSchema
}

export interface IDatasetDetailSchema extends IDatasetSchema {
    datasetName?: string
    files?: Array<IDatasetFileSchema>
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

// export interface IDatasetFullSchema extends IDatasetSchema {
//     config?: IDatasetConfigSchema
// }
