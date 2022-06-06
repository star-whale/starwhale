import { IResourceSchema } from '@/domain/base/schemas/resource'

export type IBaseImageSchema = IResourceSchema
export type IDeviceSchema = IResourceSchema

export interface IDeleteAgentSchema {
    serialNumber: string
}

export interface ISystemVersionSchema {
    id: string
    version: string
}

export interface IAgentSchema {
    connectedTime: number
    id: string
    ip: string
    serialNumber: string
    status: string
    version: string
}
