import React from 'react'
import { useQuery } from 'react-query'
import qs from 'qs'
import { tableNameOfDataset } from '@/consts'
import { scanTable } from '../services/datastore'

export function useFetchDatastore(query: any) {
    const info = useQuery(`fetchDatastore:${qs.stringify(query)}`, () => scanTable(query), {
        refetchOnWindowFocus: false,
        enabled: false,
    })
    return info
}

export function useFetchDatasetList(projectName: string, datasetName: string, datasetVersionName: string) {
    const tables = React.useMemo(
        () => [
            {
                tableName: tableNameOfDataset(projectName, datasetName, datasetVersionName),
            },
        ],
        []
    )
    const info = useFetchDatastore({
        tables,
        limit: 50,
    })

    React.useEffect(() => {
        if (projectName && datasetName && datasetVersionName) {
            info.refetch()
        }
    }, [projectName, datasetName, datasetVersionName])

    return info
}
