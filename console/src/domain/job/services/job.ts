import axios from 'axios'
import { IListQuerySchema, IListSchema } from '@/domain/base/schemas/list'
import { ICreateJobSchema, IJobSchema, IJobDetailSchema, JobActionType, IJobResultSchema } from '../schemas/job'

export async function listJobs(projectId: string, query: IListQuerySchema): Promise<IListSchema<IJobSchema>> {
    const resp = await axios.get<IListSchema<IJobSchema>>(`/api/v1/project/${projectId}/job`, {
        params: query,
    })
    return resp.data
}

export async function fetchJob(projectId: string, jobId: string): Promise<any> {
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

export async function fetchJobResult(projectId: string, jobId: string): Promise<IJobResultSchema> {
    const resp = await axios.get<IJobResultSchema>(`/api/v1/project/${projectId}/job/${jobId}/result`)
    return resp.data
}

export async function fetchJobDAG(projectId: string, jobId: string): Promise<IJobResultSchema> {
    const resp = await axios.get<IJobResultSchema>(`/api/v1/project/${projectId}/job/${jobId}/dag`)
    return resp.data
}

export async function pinJob(projectId: string, jobId: string, pinned: boolean): Promise<IJobSchema> {
    const resp = await axios.post<IJobSchema>(`/api/v1/project/${projectId}/job/${jobId}/pin`, {
        pinned,
    })
    return resp.data
}
