import { IUserSchema } from '@user/schemas/user'
import { IResourceSchema } from '@/schemas/resource'

export interface ITerminalRecordSchema extends IResourceSchema {
    creator?: IUserSchema
    resource?: IResourceSchema
    pod_name: string
    container_name: string
}
