import React, { useEffect } from 'react'
import { useQuery } from 'react-query'
import { fetchRuntimeTree } from '../services/runtime'

export function useFetchRuntimeTree(projectId: string) {
    const info = useQuery(`fetchRuntimeTree:${projectId}`, () => fetchRuntimeTree(projectId), {
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
