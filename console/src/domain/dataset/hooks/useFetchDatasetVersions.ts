import { IListQuerySchema } from '@/domain/base/schemas/list'
import { useQuery } from 'react-query'
import { listDatasetVersions } from '../services/datasetVersion'
import qs from 'qs'

export function useFetchDatasetVersions(projectId: string, datasetId: string, query: IListQuerySchema) {
    const datasetVersionsInfo = useQuery(`fetchDatasetVersions:${qs.stringify(query)}`, () =>
        listDatasetVersions(projectId, datasetId, query)
    )
    return datasetVersionsInfo
}
