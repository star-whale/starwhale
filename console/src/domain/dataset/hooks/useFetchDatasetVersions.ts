import { IListQuerySchema } from '@/domain/base/schemas/list'
import { useQuery } from 'react-query'
import qs from 'qs'
import { listDatasetVersions, listDatasetVersionsByIds } from '../services/datasetVersion'

export function useFetchDatasetVersions(projectId: string, datasetId: string, query: IListQuerySchema) {
    const datasetVersionsInfo = useQuery(`fetchDatasetVersions:${projectId}:${datasetId}:${qs.stringify(query)}`, () =>
        listDatasetVersions(projectId, datasetId, query)
    )
    return datasetVersionsInfo
}

export function useFetchDatasetVersionsByIds(projectId: string, datasetVersionIds: string, query: IListQuerySchema) {
    const datasetVersionsInfo = useQuery(
        `fetchDatasetVersions:${projectId}:${datasetVersionIds}:${qs.stringify(query)}`,
        () => listDatasetVersionsByIds(projectId, datasetVersionIds, query)
    )
    return datasetVersionsInfo
}
