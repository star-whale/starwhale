import { useRef } from 'react'
import useDatastoreMixedSchema from './useDatastoreMixedSchema'
import { useQueryDatastore } from './useFetchDatastore'
import { QueryTableRequest } from '../schemas/datastore'

export function useFetchDatastoreByTable(recordQuery: QueryTableRequest, enabled = true) {
    const recordInfo = useQueryDatastore(recordQuery, enabled)
    const { records, columnTypes } = useDatastoreMixedSchema(recordInfo?.data)

    // cache columnTypes, especially when query changed, record fetch again, columnTypes will be reset
    const columnTypesRef = useRef(columnTypes)
    if (recordInfo.isSuccess) columnTypesRef.current = columnTypes

    return {
        recordQuery,
        recordInfo,
        columnTypes: !recordInfo.isSuccess ? columnTypesRef.current : columnTypes,
        records,
    }
}

export default useFetchDatastoreByTable
