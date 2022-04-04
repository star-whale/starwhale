import { IResourceSchema } from '@/schemas/resource'
import { IUserSchema } from '@user/schemas/user'
// todo fix model & model detail has different attrs
export interface IModelSchema extends IResourceSchema {
    name: string
    createTime: number
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

// export interface IModelFullSchema extends IModelSchema {
//     config?: IModelConfigSchema
// }
