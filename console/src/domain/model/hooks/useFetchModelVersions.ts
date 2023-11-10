import { IListQuerySchema } from '@/domain/base/schemas/list'
import { useQuery } from 'react-query'
import qs from 'qs'
import { listModelVersions } from '../services/modelVersion'

export function useFetchModelVersions(projectId: string, modelId: string, query: IListQuerySchema) {
    return useQuery(`fetchModelVersions:${projectId}:${modelId}:${qs.stringify(query)}`, () =>
        listModelVersions(projectId, modelId, query)
    )
}
