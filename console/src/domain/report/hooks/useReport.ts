import { useQuery } from 'react-query'
import { listReports } from '@/domain/report/services/report'

export function useFetchReports(projectId: string, filter?: string) {
    return useQuery(['useFetchReports', projectId, filter], () => listReports(projectId, filter))
}
