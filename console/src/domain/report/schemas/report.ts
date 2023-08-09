import { IUserSchema } from '@user/schemas/user'

export interface IReportSchema {
    id: number
    uuid: string
    title: string
    description?: string
    createdTime: number
    modifiedTime: number
    owner: IUserSchema
    shared: boolean
}

export interface IReportDetailSchema extends IReportSchema {
    content: string
}
