import { IListQuerySchema } from '@/domain/base/schemas/list'
import { useQuery } from 'react-query'
import qs from 'qs'
import { listEvaluations } from '../services/evaluation'

export function useFetchEvaluations(projectId: string, query: IListQuerySchema) {
    const info = useQuery(
        `listEvaluations:${projectId}:${qs.stringify(query)}`,
        () => listEvaluations(projectId, query),
        {
            refetchOnWindowFocus: false,
        }
    )
    return info
}
