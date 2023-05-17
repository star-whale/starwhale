import { useMemo, useRef } from 'react'
import { IListQuerySchema } from '../../server/schemas/list'
import { QueryTableRequest } from '../schemas/datastore'
import useDatastoreMixedSchema from './useDatastoreMixedSchema'
import { useQueryDatastore } from './useFetchDatastore'
import { getQuery } from './useDatastoreQueryParams'

export function useFetchDatastoreByTable(
    tableName?: string,
    options?: IListQuerySchema & {
        filter?: any[]
        query?: QueryTableRequest
        revision?: string
    },
    enabled = true
) {
    const { recordQuery } = useMemo(() => {
        return getQuery({ options, tableName })
    }, [tableName, options])

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
