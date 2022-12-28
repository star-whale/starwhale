import { IResourceSchema } from '@/domain/base/schemas/resource'
import { IUserSchema } from '@user/schemas/user'

export interface ITrashSchema extends IResourceSchema {
    id: string
    name: string
    type: string
    trashedBy?: IUserSchema
    size?: number
    trashedTime?: number
    retentionTime?: number
    lastUpdatedTime?: number
}
