import { useEffect } from 'react'
import { useQuery } from 'react-query'
import { fetchDatasetVersion } from '../services/datasetVersion'

export function useFetchDatasetVersion(projectId: string, datasetId: string, datasetVersionId: string) {
    const datasetVersionsInfo = useQuery(
        `fetchDatasetVersion:${projectId}:${datasetId}:${datasetVersionId}`,
        () => fetchDatasetVersion(projectId, datasetId, datasetVersionId),
        { enabled: !!datasetVersionId }
    )

    useEffect(() => {
        if (datasetVersionId) {
            datasetVersionsInfo.refetch()
        }
    }, [datasetVersionId, datasetVersionsInfo])

    return datasetVersionsInfo
}
