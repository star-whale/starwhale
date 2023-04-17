import React, { useMemo } from 'react'
import qs from 'qs'
import { IListQuerySchema } from '../../server/schemas/list'
import { QueryTableRequest } from '../schemas/datastore'
import useDatastoreMixedSchema from './useDatastoreMixedSchema'
import { RecordSchemaT } from '../types'
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

    // React.useEffect(() => {
    //     // 1. when table changed
    //     // 2. enabled false
    //     if (recordQuery.tableName && !enabled && !recordInfo.isSuccess) {
    //         recordInfo.refetch()
    //     }
    //     // eslint-disable-next-line react-hooks/exhaustive-deps
    // }, [recordQuery, enabled, ])

    // // when table changed
    // React.useEffect(() => {
    //     if (columnQuery.tableName && !recordInfo.isSuccess) {
    //         columnInfo.refetch()
    //     }
    //     // eslint-disable-next-line react-hooks/exhaustive-deps
    // }, [columnQuery])

    return {
        recordQuery,
        recordInfo,
        columnTypes,
        records,
    }
}

export default useFetchDatastoreByTable
