import { IResourceSchema } from '@/domain/base/schemas/resource'

export interface IAgentSchema extends IResourceSchema {
    ip: string
    connectedTime: number
    status?: string
    version: string
}
