import { useEffect } from 'react'
import { useQuery } from 'react-query'
import { fetchModelVersionDiff } from '../services/modelVersion'

export function useFetchModelVersionDiff(
    projectId: string,
    modelId: string,
    modelVersionId: string,
    compareVersion?: string
) {
    const modelVersionsInfo = useQuery(
        `fetchModelVersionDiff:${projectId}:${modelId}:${modelVersionId}`,
        () => fetchModelVersionDiff(projectId, modelId, modelVersionId, compareVersion as string),
        { enabled: !!modelVersionId && !!compareVersion }
    )

    useEffect(() => {
        if (modelVersionId && compareVersion) {
            modelVersionsInfo.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [modelVersionId, compareVersion])

    return modelVersionsInfo
}
