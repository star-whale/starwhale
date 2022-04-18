import { IResourceSchema } from '@/domain/base/schemas/resource'
import { IAgentSchema } from './agent'
import { JobStatusType } from './job'

export interface ITaskSchema extends IResourceSchema {
    uuid: string
    agent: IAgentSchema
    startTime: number
    taskStatus: number
    // JobStatusType
}

export interface ITaskDetailSchema extends ITaskSchema {}

export interface IUpdateTaskSchema {}

export interface ICreateTaskSchema {}
