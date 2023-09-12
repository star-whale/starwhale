import { useQuery } from 'react-query'
import { fetchRecentRuntimeTree, fetchRuntimeTree } from '../services/runtime'

export function useFetchRuntimeTree(projectId: string) {
    const info = useQuery(`fetchRuntimeTree:${projectId}`, () => fetchRuntimeTree(projectId), {
        enabled: !!projectId,
    })

    return info
}

export function useFetchRecentRuntimeTree(projectId?: string) {
    const info = useQuery(`fetchRecentRuntimeTree:${projectId}`, () => fetchRecentRuntimeTree(projectId as string), {
        enabled: !!projectId,
    })

    return info
}
