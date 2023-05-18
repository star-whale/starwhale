import { IListQuerySchema } from '../server'
import { DataTypes } from './constants'
import { QueryTableRequest } from './schemas/datastore'

export type RecordListSchemaT = Record<string, RecordSchemaT>[]
export type ColumnSchemaT = {
    type: DataTypes
    name: string
}
export type RecordSchemaT = {
    type: DataTypes
    name: string
    value: any
}
export type DataTypeT = keyof typeof DataTypes
export type DataNameT = 'unknown' | 'int' | 'float' | 'bool' | 'string' | 'bytes'

export type DatastorePageT = IListQuerySchema & {
    filter?: any[]
    query?: QueryTableRequest
    revision?: string
}
