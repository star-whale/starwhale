import { useQuery } from 'react-query'
import { fetchReport, listReports } from '@/domain/report/services/report'
import { IListQuerySchema } from '@base/schemas/list'

export function useFetchReports(projectId: string, query: IListQuerySchema) {
    return useQuery(['useFetchReports', projectId, query], () => listReports(projectId, query))
}

export function useFetchReport(projectId: string, reportId: string) {
    return useQuery(['useFetchReport', projectId, reportId], () => fetchReport(projectId, reportId), {
        enabled: !!reportId && !!projectId,
    })
}
