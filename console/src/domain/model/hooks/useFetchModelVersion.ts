import { useEffect } from 'react'
import { useQuery } from 'react-query'
import { fetchModelVersion } from '../services/modelVersion'

export function useFetchModelVersion(projectId: string, modelId: string, modelVersionId: string) {
    const modelVersionsInfo = useQuery(
        `fetchModelVersion:${projectId}:${modelId}:${modelVersionId}`,
        () => fetchModelVersion(projectId, modelId, modelVersionId),
        { enabled: !!modelVersionId }
    )

    useEffect(() => {
        if (modelVersionId) {
            modelVersionsInfo.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [modelVersionId])

    return modelVersionsInfo
}
