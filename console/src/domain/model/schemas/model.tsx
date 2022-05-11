import { IResourceSchema } from '@/domain/base/schemas/resource'
import { IUserSchema } from '@user/schemas/user'

export interface IModelSchema extends IResourceSchema {
    name: string
    owner?: IUserSchema
}

export interface IModelDetailSchema extends IModelSchema {
    modelName?: string
    files?: Array<IModelFileSchema>
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
