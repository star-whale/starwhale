import { IBaseSchema } from './base'

export interface IResourceSchema extends IBaseSchema {
    id: string
    name: string
}

export interface IHasTagSchema {
    tag: string
    alias: string
    latest: boolean
}
