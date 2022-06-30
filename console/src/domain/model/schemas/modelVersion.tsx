import { IResourceSchema } from '@/domain/base/schemas/resource'
import { IUserSchema } from '@user/schemas/user'

export interface IModelVersionSchema extends IResourceSchema {
    name: string
    tag: string
    meta: Record<string, unknown>
    owner?: IUserSchema
}

export interface IModelVersionDetailSchema extends IModelVersionSchema {
    modelName?: string
}

export interface IUpdateModelVersionSchema {
    tag: string
}

export interface ICreateModelVersionSchema {
    modelName: string
    zipFile?: FileList
    importPath?: string
}
