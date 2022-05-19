import { IListQuerySchema } from '@/domain/base/schemas/list'
import { useQuery } from 'react-query'
import qs from 'qs'
import { listAgents, fetchSystemVersion } from '../services/system'

export function useFetchAgents(query: IListQuerySchema) {
    const info = useQuery(`fetchAgents:${qs.stringify(query)}`, () => listAgents(query))
    return info
}

export function useFetchSystemVersion() {
    const info = useQuery(`fetchSystemVersion`, () => fetchSystemVersion())
    return info
}
