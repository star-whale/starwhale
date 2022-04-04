import { IResourceSchema } from '@/schemas/resource'
import { JobStatusType } from './job'

export interface ITaskSchema extends IResourceSchema {
    uuid: string
    ip: string
    startTime: number
    status: JobStatusType
}

export interface ITaskDetailSchema extends ITaskSchema {}

export interface IUpdateTaskSchema {}

export interface ICreateTaskSchema {}
