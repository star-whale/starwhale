import { DataTypes } from './constants'

export type DataTypeT = keyof typeof DataTypes
export type DataNameT = 'unknown' | 'int' | 'float' | 'bool' | 'string' | 'bytes'
