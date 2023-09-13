import { IListQuerySchema } from '@/domain/base/schemas/list'
import { useQuery } from 'react-query'
import qs from 'qs'
import { listRuntimes } from '../services/runtime'

export function useFetchRuntimes(projectId: string, query: IListQuerySchema & { name?: string }) {
    const info = useQuery(`fetchRuntimes:${projectId}:${qs.stringify(query)}`, () => listRuntimes(projectId, query), {
        refetchOnWindowFocus: false,
    })
    return info
}
