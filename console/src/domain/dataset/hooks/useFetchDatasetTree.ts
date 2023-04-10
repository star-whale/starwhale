import { useEffect } from 'react'
import { useQuery } from 'react-query'
import { fetchDatasetTree } from '../services/dataset'

export function useFetchDatasetTree(projectId: string) {
    const datasetInfo = useQuery(`fetchDatasetTree:${projectId}`, () => fetchDatasetTree(projectId), {
        enabled: !!projectId,
    })

    useEffect(() => {
        if (projectId) {
            datasetInfo.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projectId])

    return datasetInfo
}
