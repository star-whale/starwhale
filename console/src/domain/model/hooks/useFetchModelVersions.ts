import { IListQuerySchema } from '@/domain/base/schemas/list'
import { useQuery } from 'react-query'
import { listModelVersions } from '../services/modelVersion'
import qs from 'qs'

export function useFetchModelVersions(projectId: string, modelId: string, query: IListQuerySchema) {
    const modelVersionsInfo = useQuery(`fetchModelVersions:${projectId}:${modelId}:${qs.stringify(query)}`, () =>
        listModelVersions(projectId, modelId, query)
    )
    return modelVersionsInfo
}
