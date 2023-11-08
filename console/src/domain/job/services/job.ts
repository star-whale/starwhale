import axios from 'axios'
import { IListQuerySchema, IListSchema } from '@/domain/base/schemas/list'
import {
    ICreateJobSchema,
    IJobSchema,
    IJobDetailSchema,
    JobActionType,
    IJobResultSchema,
    IExecInTaskSchema,
    IJobTemplateSchema,
    ICeateJobTemplateSchema,
    IJobEventSchema,
} from '../schemas/job'

export interface IJobListQuerySchema extends IListQuerySchema {
    swmpId?: string
}

export async function listJobs(projectId: string, query: IJobListQuerySchema): Promise<IListSchema<IJobSchema>> {
    const resp = await axios.get<IListSchema<IJobSchema>>(`/api/v1/project/${projectId}/job`, {
        params: query,
    })
    return resp.data
}

export async function fetchJob(projectId: string, jobId: string): Promise<IJobDetailSchema> {
    const resp = await axios.get<IJobDetailSchema>(`/api/v1/project/${projectId}/job/${jobId}`)
    return resp.data
}

export async function createJob(projectId: string, data: ICreateJobSchema): Promise<IJobSchema> {
    const resp = await axios.post<IJobSchema>(`/api/v1/project/${projectId}/job`, data)
    return resp.data
}

export async function doJobAction(projectId: string, jobId: string, action: JobActionType): Promise<IJobSchema> {
    const resp = await axios.post<IJobSchema>(`/api/v1/project/${projectId}/job/${jobId}/${action}`, {})
    return resp.data
}

export async function fetchJobDAG(projectId: string, jobId: string): Promise<IJobResultSchema> {
    const resp = await axios.get<IJobResultSchema>(`/api/v1/project/${projectId}/job/${jobId}/dag`)
    return resp.data
}

export async function fetchJobEvents(
    projectId: string,
    jobId: string,
    taskId?: string,
    runId?: string
): Promise<IJobEventSchema[]> {
    const resp = await axios.get<IJobEventSchema[]>(`/api/v1/project/${projectId}/job/${jobId}/event`, {
        params: {
            taskId,
            runId,
        },
    })
    return resp.data
}

export async function executeInTask(
    projectId: string,
    jobId: string,
    taskId: string,
    cmd: string
): Promise<IExecInTaskSchema> {
    const resp = await axios.post<IExecInTaskSchema>(`/api/v1/project/${projectId}/job/${jobId}/task/${taskId}/exec`, {
        command: cmd.split(' '),
    })
    return resp.data
}

export async function pinJob(projectId: string, jobId: string, pinned: boolean): Promise<IJobSchema> {
    const resp = await axios.post<IJobSchema>(`/api/v1/project/${projectId}/job/${jobId}/pin`, {
        pinned,
    })
    return resp.data
}

export async function fetchJobTemplate(projectId: string, templateId: string): Promise<any> {
    const resp = await axios.get<IJobTemplateSchema>(`/api/v1/project/${projectId}/template/${templateId}`)
    return resp.data
}

export async function listJobTemplate(projectId: string): Promise<any> {
    const resp = await axios.get<IJobTemplateSchema>(`/api/v1/project/${projectId}/template`)
    return resp.data
}

export async function listJobRecentTemplate(projectId: string): Promise<any> {
    const resp = await axios.get<IJobTemplateSchema>(`/api/v1/project/${projectId}/recent-template`)
    return resp.data
}

export async function createJobTemplate(projectId: string, data: ICeateJobTemplateSchema): Promise<any> {
    const resp = await axios.post<ICeateJobTemplateSchema>(`/api/v1/project/${projectId}/template`, data)
    return resp.data
}
