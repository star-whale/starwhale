import axios from 'axios'
import { IListQuerySchema } from '@/domain/base/schemas/list'
import { JobActionType } from '../schemas/job'
import {
    ICreateJobTemplateRequest,
    IEventVo,
    IExecResponse,
    IGraph,
    IJobRequest,
    IJobTemplateVo,
    IJobVo,
    IPageInfoJobVo,
} from '@/api'

export interface IJobListQuerySchema extends IListQuerySchema {
    swmpId?: string
}

export async function listJobs(projectId: string, query: IJobListQuerySchema): Promise<IPageInfoJobVo> {
    const resp = await axios.get<IPageInfoJobVo>(`/api/v1/project/${projectId}/job`, {
        params: query,
    })
    return resp.data
}

export async function fetchJob(projectId: string, jobId: string): Promise<IJobVo> {
    const resp = await axios.get<IJobVo>(`/api/v1/project/${projectId}/job/${jobId}`)
    return resp.data
}

export async function createJob(projectId: string, data: IJobRequest): Promise<string> {
    const resp = await axios.post<string>(`/api/v1/project/${projectId}/job`, data)
    return resp.data
}

export async function doJobAction(projectId: string, jobId: string, action: JobActionType): Promise<string> {
    const resp = await axios.post<string>(`/api/v1/project/${projectId}/job/${jobId}/${action}`, {})
    return resp.data
}

export async function fetchJobDAG(projectId: string, jobId: string): Promise<IGraph> {
    const resp = await axios.get<IGraph>(`/api/v1/project/${projectId}/job/${jobId}/dag`)
    return resp.data
}

export async function fetchJobEvents(
    projectId: string,
    jobId: string,
    taskId?: string,
    runId?: string
): Promise<IEventVo[]> {
    const resp = await axios.get<IEventVo[]>(`/api/v1/project/${projectId}/job/${jobId}/event`, {
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
): Promise<IExecResponse> {
    const resp = await axios.post<IExecResponse>(`/api/v1/project/${projectId}/job/${jobId}/task/${taskId}/exec`, {
        command: cmd.split(' '),
    })
    return resp.data
}

export async function pinJob(projectId: string, jobId: string, pinned: boolean): Promise<string> {
    const resp = await axios.post<string>(`/api/v1/project/${projectId}/job/${jobId}/pin`, {
        pinned,
    })
    return resp.data
}

export async function fetchJobTemplate(projectId: string, templateId: string): Promise<IJobTemplateVo> {
    const resp = await axios.get<IJobTemplateVo>(`/api/v1/project/${projectId}/template/${templateId}`)
    return resp.data
}

export async function listJobTemplate(projectId: string): Promise<IJobTemplateVo[]> {
    const resp = await axios.get<IJobTemplateVo[]>(`/api/v1/project/${projectId}/template`)
    return resp.data
}

export async function listJobRecentTemplate(projectId: string): Promise<IJobTemplateVo[]> {
    const resp = await axios.get<IJobTemplateVo[]>(`/api/v1/project/${projectId}/recent-template`)
    return resp.data
}

export async function createJobTemplate(projectId: string, data: ICreateJobTemplateRequest): Promise<any> {
    const resp = await axios.post<ICreateJobTemplateRequest>(`/api/v1/project/${projectId}/template`, data)
    return resp.data
}
