import { IListQuerySchema } from '@/domain/base/schemas/list'
import { useQuery } from 'react-query'
import qs from 'qs'
import {
    listAgents,
    fetchSystemVersion,
    fetchSystemSetting,
    fetchSystemResourcePool,
    fetchSystemFeatures,
} from '../services/system'

export function useFetchAgents(query: IListQuerySchema) {
    const info = useQuery(`fetchAgents:${qs.stringify(query)}`, () => listAgents(query))
    return info
}

export function useFetchSystemVersion() {
    const info = useQuery('fetchSystemVersion', () => fetchSystemVersion(), { staleTime: Infinity })
    return info
}

export function useFetchSystemSetting() {
    const info = useQuery('fetchSystemSetting', () => fetchSystemSetting())
    return info
}

export function useFetchSystemFeatures() {
    const info = useQuery('fetchSystemFeatures', () => fetchSystemFeatures())
    return info
}

export function useFetchSystemResourcePool() {
    const info = useQuery('fetchSystemResourcePool', () => fetchSystemResourcePool())
    return info
}
