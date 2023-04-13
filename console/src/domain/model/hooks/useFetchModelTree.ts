import { useEffect } from 'react'
import { useQuery } from 'react-query'
import { fetchModelTree } from '../services/model'

export function useFetchModelTree(projectId: string) {
    const info = useQuery(`fetchModelTree:${projectId}`, () => fetchModelTree(projectId), {
        enabled: !!projectId,
    })

    useEffect(() => {
        if (projectId) {
            info.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projectId])

    return info
}
