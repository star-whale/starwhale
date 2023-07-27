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
    READY = 'READY',
    PAUSED = 'PAUSED',
    RUNNING = 'RUNNING',
    TO_CANCEL = 'TO_CANCEL',
    CANCELLING = 'CANCELLING',
    CANCELED = 'CANCELED',
    SUCCESS = 'SUCCESS',
    FAIL = 'FAIL',
    UNKNOWN = 'UNKNOWN',
}

export enum ExposedLinkType {
    DEV_MODE = 'DEV_MODE',
    WEB_HANDLER = 'WEB_HANDLER',
}

export interface IExposedLinkSchema {
    type: ExposedLinkType
    name: string
    link: string
}

export enum RuntimeType {
    BUILTIN = 'builtin',
    OTHER = 'other',
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
    deviceAmount?: number
    duration: number
    jobStatus: JobStatusType
    resourcePool?: string
    comment?: string
    stopTime?: number
    createdTime?: number
    pinnedTime?: number
    exposedLinks?: IExposedLinkSchema[]
    isTimeToLiveInSec?: boolean
    timeToLiveInSec?: number
}

export type IJobDetailSchema = IJobSchema

export interface ICreateJobSchema {
    modelVersionUrl: string
    datasetVersionUrls?: string
    runtimeVersionUrl?: string
    resourcePool?: string
    stepSpecOverWrites?: string
    devMode?: boolean
    devPassword?: string
    isTimeToLiveInSec?: boolean
    timeToLiveInSec?: number
}

// export interface IJobFormSchema extends IJobSchema {
//     modelId: string
//     runtimeId: string
//     runtimeVersionUrl: string
//     datasetId: string
//     datasetVersionId: string
//     datasetVersionIdsArr?: Array<string>
// }

export interface ICreateJobFormSchema extends Omit<ICreateJobSchema, 'stepSpecOverWrites'> {
    runtimeVersionUrl: string
    runtimeType?: string
    datasetId: string
    datasetVersionId: string
    datasetVersionUrls?: Array<string>
    resourcePool: string
    resourcePoolTmp?: string
    stepSpecOverWrites: string
    rawType: boolean
    modelVersionHandler: string
    devMode: boolean
    devPassword: string
    isTimeToLiveInSec?: boolean
    timeToLiveInSec?: number
}

export type IJobResultSchema = any

export interface IExecInTaskSchema {
    stdout: string
    stderr: string
}
