import { IResourceSchema } from '@/domain/base/schemas/resource'
import { JobStatusType } from './job'

export interface IAgentSchema extends IResourceSchema {
    ip: string
    connectedTime: number
    status?: string
    version: string
}
