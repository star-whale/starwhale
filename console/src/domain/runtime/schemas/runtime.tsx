import { IResourceSchema } from '@/domain/base/schemas/resource'

export type IBaseImageSchema = IResourceSchema
export type IDeviceSchema = IResourceSchema

export interface ICreateBaseImageSchema {
    imageName: string
}
