import { IResourceSchema } from '@/domain/base/schemas/resource'
import { IUserSchema } from '@user/schemas/user'
import { IDatasetFileSchema, IDatasetTreeVersionSchema, IDatasetVersionSchema } from './datasetVersion'

export interface IDatasetSchema extends IResourceSchema {
    name: string
    owner?: IUserSchema
    version?: IDatasetVersionSchema
}

export interface IDatasetDetailSchema {
    id?: string
    name?: string
    createdTime?: number
    versionMeta?: string
    versionName?: string
    versionTag?: string
    versionAlias?: string
    files?: Array<IDatasetFileSchema>
    indexTable?: string
    shared?: number
}

export interface IUpdateDatasetSchema {
    description?: string
}
export interface ICreateDatasetFormSchema {
    datasetName: string
    shared?: number
    upload?: {
        storagePath?: string
        type?: string
    }
}

export interface ICreateDatasetQuerySchema {
    datasetId?: string
    shared?: number
    type: 'IMAGE' | 'VIDEO' | 'AUDIO'
    storagePath: string
}

export interface IDatasetTreeSchema {
    ownerName: string
    projectName: string
    datasetName: string
    shared: number
    versions: IDatasetTreeVersionSchema[]
}

export interface IDatasetTaskBuildSchema {
    id: string
    datasetId: string
    projectId: string
    taskId: string
    datasetName: string
    status: TaskBuildStatusType
    type: string
    createTime: number
}

export enum TaskBuildStatusType {
    CREATED = 'CREATED',
    UPLOADING = 'UPLOADING',
    BUILDING = 'BUILDING',
    RUNNING = 'RUNNING',
    SUCCESS = 'SUCCESS',
    FAILED = 'FAILED',
}
