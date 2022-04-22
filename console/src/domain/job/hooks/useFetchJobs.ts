import { IListQuerySchema } from '@/domain/base/schemas/list'
import { useQuery } from 'react-query'
import { listJobs } from '../services/job'
import qs from 'qs'

export function useFetchJobs(projectId: string, query: IListQuerySchema) {
    const jobsInfo = useQuery(`fetchJobs:${projectId}:${qs.stringify(query)}`, () => listJobs(projectId, query))
    return jobsInfo
}
