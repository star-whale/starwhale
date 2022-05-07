export interface IListSchema<T> {
    total?: number
    pageNum?: number
    pageSize?: number
    size?: number
    list: T[]
}

export interface IListQuerySchema {
    pageNum: number
    pageSize: number
    search?: string
}
