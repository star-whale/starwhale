import { useEffect } from 'react'
import { useQuery } from 'react-query'
import { listModelVersionFiles } from '../services/modelVersion'
import qs from 'qs'

export function useFetchModelVersionFiles(projectId: string, modelName: string, modelVersionId: string, path?: string) {
    const params = {
        version: modelVersionId,
        path,
    }
    const modelVersionFilesInfo = useQuery(
        `fetchModelVersion:${projectId}:${modelName}:${qs.stringify(params)}`,
        () => listModelVersionFiles(projectId, modelName, params),
        {
            enabled: false,
        }
    )

    useEffect(() => {
        if (projectId && modelName && modelVersionId) {
            modelVersionFilesInfo.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [modelVersionId])

    return modelVersionFilesInfo
}
