import { IListQuerySchema } from '@/domain/base/schemas/list'
import { useQuery } from 'react-query'
import qs from 'qs'
import { listDatasetVersions } from '../services/datasetVersion'

export function useFetchDatasetVersions(projectId: string, datasetId: string, query: IListQuerySchema) {
    return useQuery(`fetchDatasetVersions:${projectId}:${datasetId}:${qs.stringify(query)}`, () =>
        listDatasetVersions(projectId, datasetId, query)
    )
}
