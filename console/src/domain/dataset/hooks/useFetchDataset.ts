import { useEffect } from 'react'
import { useQuery } from 'react-query'
import { fetchDataset } from '../services/dataset'

export function useFetchDataset(projectId: string, datasetId: string) {
    const datasetInfo = useQuery(
        `fetchDatasetVersion:${projectId}:${datasetId}`,
        () => fetchDataset(projectId, datasetId),
        { enabled: !!datasetId }
    )

    useEffect(() => {
        if (datasetId) {
            datasetInfo.refetch()
        }
    }, [datasetId])

    return datasetInfo
}
