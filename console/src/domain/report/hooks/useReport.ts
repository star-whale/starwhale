import { useQuery } from 'react-query'
import { listReports } from '@/domain/report/services/report'
import { IListQuerySchema } from '@base/schemas/list'

export function useFetchReports(projectId: string, query: IListQuerySchema) {
    return useQuery(['useFetchReports', projectId, query], () => listReports(projectId, query))
}
