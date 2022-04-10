import axios from 'axios'
import { ITaskSchema, ITaskDetailSchema } from '../schemas/task'
import { IListQuerySchema, IListSchema } from '@/domain/base/schemas/list'

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

export async function fetchTask(projectId: string, jobId: string, taskId: string): Promise<any> {
    const resp = await axios.get<ITaskDetailSchema>(`/api/v1/project/${projectId}/job/${jobId}/task/${taskId}`)
    return resp.data
}
