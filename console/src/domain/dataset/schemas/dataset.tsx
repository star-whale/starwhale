export interface ICreateDatasetFormSchema {
    datasetName: string
    shared?: number
    upload?: {
        storagePath?: string
        type?: string
    }
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
