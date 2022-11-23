import { IResourceSchema } from '@/domain/base/schemas/resource';
import { IAgentSchema } from './agent';
export declare enum TaskStatusType {
    CREATED = "CREATED",
    PAUSED = "PAUSED",
    PREPARING = "PREPARING",
    RUNNING = "RUNNING",
    SUCCESS = "SUCCESS",
    TO_CANCEL = "TO_CANCEL",
    CANCELLING = "CANCELLING",
    CANCELED = "CANCELED",
    FAIL = "FAIL",
    UNKNOWN = "UNKNOWN"
}
export interface ITaskSchema extends IResourceSchema {
    uuid: string;
    resourcePool: string;
    agent: IAgentSchema;
    taskStatus: TaskStatusType;
}
export declare type ITaskDetailSchema = ITaskSchema;
