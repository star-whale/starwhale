import { useQuery } from 'react-query'
import qs from 'qs'
import { IJobListQuerySchema, listJobs } from '../services/job'

export function useFetchJobs(projectId: string, query: IJobListQuerySchema) {
    return useQuery(`fetchJobs:${projectId}:${qs.stringify(query)}`, () => listJobs(projectId, query), {
        refetchOnWindowFocus: false,
    })
}
