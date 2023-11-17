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
    PREPARING = 'PREPARING',
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

export enum EventType {
    INFO = 'INFO',
    WARNING = 'WARNING',
    ERROR = 'ERROR',
}

export enum EventSource {
    CLIENT = 'CLIENT',
    SERVER = 'SERVER',
    NODE = 'NODE',
}

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

export interface ICreateJobFormSchema extends Omit<ICreateJobSchema, 'datasetVersionUrls'> {
    runtimeVersionUrl: string
    runtimeType?: string
    datasetId: string
    datasetVersionId: string
    datasetVersionUrls?: Array<string>
    validationDatasetVersionUrls?: Array<string>
    resourcePool: string
    resourcePoolTmp?: string
    stepSpecOverWrites: string
    rawType: boolean
    modelVersionHandler: string
    devMode: boolean
    devPassword: string
    isTimeToLiveInSec?: boolean
    timeToLiveInSec?: number
    templateId?: number
}

export interface IJobEventSchema {
    eventType: EventType
    source: EventSource
    message: string
    data: string
    timestamp: number
    id: number
    relatedResource: {
        eventResourceType: string
        id: number
    }
}
