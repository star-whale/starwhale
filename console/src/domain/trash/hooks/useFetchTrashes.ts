import { IListQuerySchema } from '@/domain/base/schemas/list'
import { useQuery } from 'react-query'
import qs from 'qs'
import { listTrashes } from '../services/trash'

export function useFetchTrashes(projectId: string, query: IListQuerySchema) {
    return useQuery(`fetchTrashes:${qs.stringify(query)}`, () => listTrashes(projectId, query))
}
