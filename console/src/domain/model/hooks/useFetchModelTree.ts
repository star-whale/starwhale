import { useQuery } from 'react-query'
import { fetchModelTree, fetchRecentModelTree } from '../services/model'

export function useFetchModelTree(projectId: string) {
    const info = useQuery(`fetchModelTree:${projectId}`, () => fetchModelTree(projectId), {
        enabled: !!projectId,
    })

    return info
}

export function useFetchRecentModelTree(projectId?: string) {
    const info = useQuery(`fetchRecentModelTree:${projectId}`, () => fetchRecentModelTree(projectId as string), {
        enabled: !!projectId,
    })

    return info
}
