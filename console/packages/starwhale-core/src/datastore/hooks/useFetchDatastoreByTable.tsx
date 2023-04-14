import React, { useMemo } from 'react'
import qs from 'qs'
import { IListQuerySchema } from '../../server/schemas/list'
import { QueryTableRequest } from '../schemas/datastore'
import useDatastore from './useDatastore'
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
    const { columnQuery, recordQuery } = useMemo(() => {
        return getQuery({ options, tableName })
    }, [tableName, options])

    const columnInfo = useQueryDatastore(columnQuery, enabled)
    const recordInfo = useQueryDatastore(recordQuery, enabled)

    const { records } = useDatastore(recordInfo?.data?.records)

    React.useEffect(() => {
        if (tableName && enabled && columnInfo.isError) {
            columnInfo.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [tableName, enabled])

    // when fetch error
    React.useEffect(() => {
        if (tableName && enabled && recordInfo.isError) {
            recordInfo.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [tableName, enabled])

    // when table changed
    React.useEffect(() => {
        if (recordQuery.tableName && enabled && !recordInfo.isSuccess) {
            recordInfo.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [recordQuery])

    return {
        recordQuery,
        columnInfo,
        recordInfo,
        columnTypes: (columnInfo.data?.columnTypes as RecordSchemaT[]) ?? [],
        records,
    }
}

export default useFetchDatastoreByTable
