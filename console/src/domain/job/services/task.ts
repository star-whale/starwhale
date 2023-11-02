import axios from 'axios'
import { IListQuerySchema, IListSchema } from '@/domain/base/schemas/list'
import { ITaskSchema, ITaskDetailSchema } from '../schemas/task'

export async function listTasks(
    projectId: string,
    jobId: string,
    query: IListQuerySchema
): Promise<IListSchema<ITaskSchema>> {
    const resp = await axios.get<IListSchema<ITaskSchema>>(`/api/v1/project/${projectId}/job/${jobId}/task`, {
        params: query,
    })
    return resp.data
}

export async function fetchTask(projectId: string, jobId: string, taskId: string): Promise<ITaskSchema> {
    const resp = await axios.get<ITaskDetailSchema>(`/api/v1/project/${projectId}/job/${jobId}/task/${taskId}`)
    return resp.data
}

export async function fetchTaskOfflineLogFiles(taskId: string): Promise<any> {
    const resp = await axios.get<string[]>(`/api/v1/log/offline/${taskId}`)
    return resp.data
}

export async function fetchTaskOfflineFileLog(taskId: string, fileId: string): Promise<any> {
    const resp = await axios.get<string>(`/api/v1/log/offline/${taskId}/${fileId}`)
    return resp.data
}
