import { useQuery } from 'react-query'
import { fetchResourcePool } from '../services/job'

export function useFetchResourcePools() {
    return useQuery('fetchResourcePool', () => fetchResourcePool(), {
        refetchOnWindowFocus: false,
    })
}
