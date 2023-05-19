import { useScanDatastore } from './useFetchDatastore'
import useDatastoreMixedSchema from './useDatastoreMixedSchema'
import { ScanTableRequest } from '../schemas/datastore'
import { useRef } from 'react'

export function useFetchDatastoreByTables(recordQuery: ScanTableRequest) {
    const recordInfo = useScanDatastore(recordQuery, true)
    const { records, columnTypes } = useDatastoreMixedSchema(recordInfo?.data)

    // cache columnTypes, especially when query changed, record fetch again, columnTypes will be reset
    const columnTypesRef = useRef(columnTypes)
    if (recordInfo.isSuccess) columnTypesRef.current = columnTypes

    return {
        recordInfo,
        records,
        columnTypes: !recordInfo.isSuccess ? columnTypesRef.current : columnTypes,
    }
}

export default useFetchDatastoreByTables
