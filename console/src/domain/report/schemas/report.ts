export interface IReportSchema {
    id: number
    title: string
    description?: string
    createdTime: string
    updatedTime: string
    owner: string
    shared: number
}

export interface IReportDetailSchema extends IReportSchema {
    content: string
}
