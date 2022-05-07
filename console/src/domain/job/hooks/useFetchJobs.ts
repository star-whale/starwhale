import { IListQuerySchema } from '@/domain/base/schemas/list'
import { useQuery } from 'react-query'
import qs from 'qs'
import { listJobs } from '../services/job'

export function useFetchJobs(projectId: string, query: IListQuerySchema) {
    const jobsInfo = useQuery(`fetchJobs:${projectId}:${qs.stringify(query)}`, () => listJobs(projectId, query))
    return jobsInfo
}
