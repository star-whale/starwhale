export interface IListSchema<T> {
    total: number
    pageNum: number
    size: number
    list: T[]
}

export interface IListQuerySchema {
    start: number
    count: number
    search?: string
    q?: string
    sort_by?: string
    sort_asc?: boolean
}
