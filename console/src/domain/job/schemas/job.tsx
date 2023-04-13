import { IResourceSchema } from '@/domain/base/schemas/resource'
import { IDeviceSchema } from '@/domain/setting/schemas/system'
import { IRuntimeSchema } from '@/domain/runtime/schemas/runtime'
import { IUserSchema } from '@user/schemas/user'
import { StepSpec } from '../../model/schemas/modelVersion'

export enum JobActionType {
    CANCEL = 'cancel',
    PAUSE = 'pause',
    RESUME = 'resume',
}
export enum JobStatusType {
    CREATED = 'CREATED',
    PAUSED = 'PAUSED',
    RUNNING = 'RUNNING',
    TO_CANCEL = 'TO_CANCEL',
    CANCELING = 'CANCELING',
    CANCELED = 'CANCELED',
    TO_COLLECT_RESULT = 'TO_COLLECT_RESULT',
    COLLECTING_RESULT = 'COLLECTING_RESULT',
    SUCCESS = 'SUCCESS',
    FAIL = 'FAIL',
    UNKNOWN = 'UNKNOWN',
}

export interface IJobSchema extends IResourceSchema {
    uuid: string
    name: string
    owner?: IUserSchema
    modelName?: string
    modelVersion?: string
    datasets?: string[]
    runtime?: IRuntimeSchema
    device?: IDeviceSchema
    deviceAmount: number
    duration: number
    createdTime: number
    stopTime: number
    jobStatus: JobStatusType
}

export type IJobDetailSchema = IJobSchema

export interface ICreateJobSchema {
    modelVersionUrl: string
    datasetVersionUrls?: string
    runtimeVersionUrl?: string
    resourcePool?: string
    stepSpecOverWrites?: string
}

export interface IJobFormSchema extends IJobSchema {
    modelId: string
    runtimeId: string
    runtimeVersionUrl: string
    datasetId: string
    datasetVersionId: string
    datasetVersionIdsArr?: Array<string>
}

export interface ICreateJobFormSchema extends Omit<ICreateJobSchema, 'stepSpecOverWrites'> {
    modelId: string
    runtimeId: string
    runtimeVersionUrl: string
    datasetId: string
    datasetVersionId: string
    datasetVersionIdsArr?: Array<string>
    resourcePool: string
    stepSpecOverWrites: StepSpec[]
    rawType: boolean
    modelVersionHandler: string
}

export type IJobResultSchema = any
