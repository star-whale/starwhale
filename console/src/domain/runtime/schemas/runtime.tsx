import { IResourceSchema } from '@/schemas/resource'

export interface IBaseImageSchema extends IResourceSchema {
    id: string
    name: string
}
export interface IDeviceSchema extends IResourceSchema {
    id: string
    name: string
}
