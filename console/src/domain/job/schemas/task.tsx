import { IResourceSchema } from '@/domain/base/schemas/resource'
import { IAgentSchema } from './agent'
import { JobStatusType } from './job'

export interface ITaskSchema extends IResourceSchema {
    uuid: string
    agent: IAgentSchema
    startTime: number
    status: JobStatusType
}

export interface ITaskDetailSchema extends ITaskSchema {}

export interface IUpdateTaskSchema {}

export interface ICreateTaskSchema {}
