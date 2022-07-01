import { IListQuerySchema } from '@/domain/base/schemas/list'
import { useQuery } from 'react-query'
import qs from 'qs'
import { listEvaluationAttrs } from '../services/evaluation'

export function useFetchEvaluationAttrs(projectId: string, query: IListQuerySchema) {
    const info = useQuery(
        `listEvaluationAttrs:${projectId}:${qs.stringify(query)}`,
        () => listEvaluationAttrs(projectId, query),
        {
            refetchOnWindowFocus: false,
        }
    )
    return info
}
