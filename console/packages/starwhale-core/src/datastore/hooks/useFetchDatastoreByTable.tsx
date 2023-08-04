import { useRef } from 'react'
import useDatastoreMixedSchema from './useDatastoreMixedSchema'
import { useQueryDatastore, useScanDatastore } from './useFetchDatastore'
import { QueryTableRequest, ScanTableRequest } from '../schemas/datastore'

export function useCombine(options: any, enabled: boolean) {
    const use = options && options.tables ? useScanDatastore : useQueryDatastore
    const info = use(options, enabled && !!options)
    return info
}

export function useFetchDatastoreByTable(recordQuery: QueryTableRequest | ScanTableRequest, enabled = true) {
    const recordInfo = useCombine(recordQuery, enabled)
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
