import { IListQuerySchema } from '@/domain/base/schemas/list'
import { useQuery } from 'react-query'
import qs from 'qs'
import { listTasks } from '../services/task'

export function useFetchTasks(projectId: string, jobId: string, query: IListQuerySchema) {
    const tasksInfo = useQuery(`fetchTasks:${projectId}:${jobId}:${qs.stringify(query)}`, () =>
        listTasks(projectId, jobId, query)
    )
    return tasksInfo
}
