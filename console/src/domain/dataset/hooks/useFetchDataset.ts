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
        // eslint-disalbe-next-line react-hooks/exhaustive-deps
    }, [datasetId])

    return datasetInfo
}
