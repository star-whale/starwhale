import { useQuery } from 'react-query'
import qs from 'qs'
import { getEvaluationViewConfig } from '../services/evaluation'

export function useFetchViewConfig(projectId: string, query: any) {
    const info = useQuery(
        `getEvaluationViewConfig:${projectId}:${qs.stringify(query)}`,
        () => getEvaluationViewConfig(projectId, query),
        {
            refetchOnWindowFocus: false,
        }
    )
    return info
}
