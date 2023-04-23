import { useScanDatastore } from './useFetchDatastore'
import { TableScanParamsT, useDatastoreScanParams } from './useDatastoreQueryParams'
import useDatastoreMixedSchema from './useDatastoreMixedSchema'

export function useFetchDatastoreByTables(queries: TableScanParamsT) {
    const { recordQuery } = useDatastoreScanParams(queries)
    const recordInfo = useScanDatastore(recordQuery, true)
    const { records, columnTypes } = useDatastoreMixedSchema(recordInfo?.data)

    return {
        recordInfo,
        records,
        columnTypes,
    }
}

export default useFetchDatastoreByTables
