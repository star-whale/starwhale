import { useScanDatastore } from './useFetchDatastore'
import useDatastoreMixedSchema from './useDatastoreMixedSchema'
import { ScanTableRequest } from '../schemas/datastore'

export function useFetchDatastoreByTables(recordQuery: ScanTableRequest) {
    const recordInfo = useScanDatastore(recordQuery, true)
    const { records, columnTypes } = useDatastoreMixedSchema(recordInfo?.data)

    return {
        recordInfo,
        records,
        columnTypes,
    }
}

export default useFetchDatastoreByTables
