import { IResourceSchema } from '@/domain/base/schemas/resource'
import { IAgentSchema } from './agent'

export enum TaskStatusType {
    CREATED = 'CREATED',
    ASSIGNING = 'ASSIGNING',
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
    agent: IAgentSchema
    startTime: number
    taskStatus: TaskStatusType
}

export type ITaskDetailSchema = ITaskSchema
