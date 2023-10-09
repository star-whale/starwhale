import { IResourceSchema } from '@/domain/base/schemas/resource'
import { IDeviceSchema } from '@/domain/setting/schemas/system'
import { IRuntimeSchema } from '@/domain/runtime/schemas/runtime'
import { IUserSchema } from '@user/schemas/user'
import { IModelSchema } from '../../model/schemas/model'

export enum EvaluationStatusType {
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

export interface IEvaluationAttributeValue {
    name: string
    type: string
    value?: string
}

export interface IEvaluationSchema extends IResourceSchema {
    jobUuid: string
    projectId: string
    projectName: string
    name: string
    owner?: IUserSchema
    model: IModelSchema
    datasets?: string[]
    runtime?: IRuntimeSchema
    device?: IDeviceSchema
    deviceAmount: number
    duration: number
    // createdTime: number
    // stopTime: number
    attributes: IEvaluationAttributeValue[]
}

export type IEvaluationViewSchema = any
