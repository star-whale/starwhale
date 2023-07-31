import { IResourceSchema } from '@/domain/base/schemas/resource'
import { IAgentSchema } from './agent'
import { IExposedLinkSchema } from '@job/schemas/job'

export enum TaskStatusType {
    CREATED = 'CREATED',
    PAUSED = 'PAUSED',
    PREPARING = 'PREPARING',
    RUNNING = 'RUNNING',
    SUCCESS = 'SUCCESS',
    TO_CANCEL = 'TO_CANCEL',
    CANCELLING = 'CANCELLING',
    CANCELED = 'CANCELED',
    FAIL = 'FAIL',
    UNKNOWN = 'UNKNOWN',
}

export interface ITaskSchema extends IResourceSchema {
    uuid: string
    resourcePool: string
    agent: IAgentSchema
    taskStatus: TaskStatusType
    stepName: string
    retryNum: number
    exposedLinks?: IExposedLinkSchema[]
    startedTime: number
    finishedTime: number
    failedReason?: string
}

export type ITaskDetailSchema = ITaskSchema
