import { IUserSchema } from '@user/schemas/user'

export interface IEventSchema {
    name: string
    operation_name: string
    updated_at: string
    creator?: IUserSchema
}
