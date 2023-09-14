import { useQuery } from 'react-query'
import { fetchDatasetTree, fetchRecentDatasetTree } from '../services/dataset'

export function useFetchDatasetTree(projectId: string) {
    const datasetInfo = useQuery(`fetchDatasetTree:${projectId}`, () => fetchDatasetTree(projectId), {
        enabled: !!projectId,
    })

    return datasetInfo
}

export function useFetchRecentDatasetTree(projectId?: string) {
    const datasetInfo = useQuery(
        `fetchRecentDatasetTree:${projectId}`,
        () => fetchRecentDatasetTree(projectId as string),
        {
            enabled: !!projectId,
        }
    )

    return datasetInfo
}
