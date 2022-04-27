import { IResourceSchema } from '@/domain/base/schemas/resource'
import { IUserSchema } from '@user/schemas/user'
import { IModelSchema } from '@model/schemas/model'
import { IDatasetSchema } from '@dataset/schemas/dataset'
import { IModelVersionSchema } from '@/domain/model/schemas/modelVersion'
import { IBaseImageSchema, IDeviceSchema } from '../../runtime/schemas/runtime'
import { IIndicator } from '@/components/Indicator/types'

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
    dataset?: IDatasetSchema
    baseImage?: IBaseImageSchema
    device?: IDeviceSchema
    deviceCount: number
    duration: number
    createTime: number
    stopTime: number
    jobStatus: JobStatusType
}

export interface IJobDetailSchema extends IJobSchema {}

export interface IUpdateJobSchema {}

export interface ICreateJobSchema {
    modelVersionId: string
    datasetVersionIds?: string
    baseImageId?: string
    deviceId?: string
    deviceCount?: number
    // resultOutputPath?: string
}

export interface IJobFormSchema extends IJobSchema {
    modelId: string
    datasetId: string
    datasetVersionId: string
    datasetVersionIdsArr?: Array<string>
}
export interface ICreateJobFormSchema extends ICreateJobSchema {
    modelId: string
    datasetId: string
    datasetVersionId: string
    datasetVersionIdsArr?: Array<string>
}

export type IJobResultSchema = any
