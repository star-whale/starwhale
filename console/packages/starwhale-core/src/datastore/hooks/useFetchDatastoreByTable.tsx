import { useRef, useCallback } from 'react'
import useDatastoreMixedSchema from './useDatastoreMixedSchema'
import { useQueryDatastore, useScanDatastore } from './useFetchDatastore'
import { ColumnSchemaDesc, QueryTableRequest, ScanTableRequest } from '../schemas/datastore'

export function useCombine(options: any, enabled: boolean) {
    const use = options && options.tables ? useScanDatastore : useQueryDatastore
    const info = use(options, enabled && !!options)
    return info
}

export function useFetchDatastoreByTable(recordQuery: QueryTableRequest | ScanTableRequest, enabled = true) {
    const recordInfo = useCombine(recordQuery, enabled)
    const { records, columnTypes, lastKey } = useDatastoreMixedSchema(recordInfo?.data)

    // cache columnTypes, especially when query changed, record fetch again, columnTypes will be reset
    const columnTypesRef = useRef(columnTypes)
    if (recordInfo.isSuccess) columnTypesRef.current = columnTypes

    return {
        lastKey,
        recordQuery,
        recordInfo,
        columnTypes: !recordInfo.isSuccess ? columnTypesRef.current : columnTypes,
        records,
        getTableRecordMap: useCallback(() => {
            if (!recordQuery) return {}
            if ('tableName' in recordQuery && recordQuery?.tableName)
                return {
                    [recordQuery.tableName]: records,
                }

            if ('tables' in recordQuery && recordQuery?.tables) {
                const m = {}
                recordQuery.tables.forEach(({ tableName = '', columnPrefix = '' }) => {
                    m[tableName] = []
                    records.forEach((record) => {
                        const newRecord = {}
                        Object.keys(record).forEach((key) => {
                            if (key.startsWith(columnPrefix)) {
                                newRecord[key.replace(columnPrefix, '')] = record[key]
                            }
                        })

                        if (Object.keys(newRecord).length !== 0) m[tableName].push(newRecord)
                    })
                })
                return m
            }
            return {}
        }, [records, recordQuery]),
        getTableColumnTypeMap: useCallback(() => {
            if (!recordQuery) return {}
            if ('tableName' in recordQuery && recordQuery?.tableName)
                return {
                    [recordQuery.tableName]: columnTypes,
                }

            if ('tables' in recordQuery && recordQuery?.tables) {
                const m = {}
                recordQuery.tables.forEach(({ tableName = '', columnPrefix = '' }) => {
                    const newColumnType: ColumnSchemaDesc[] = []

                    m[tableName] = columnTypes
                        .map((columnType) => {
                            if (columnType.name.startsWith(columnPrefix)) {
                                newColumnType.push({
                                    ...columnType,
                                    name: columnType.name.replace(columnPrefix, ''),
                                })
                            }
                            return newColumnType
                        })
                        .flat()
                })
                return m
            }
            return {}
        }, [columnTypes, recordQuery]),
        getTableDistinctColumnTypes: useCallback((): ColumnSchemaDesc[] => {
            if (!recordQuery) return []
            if ('tableName' in recordQuery && recordQuery?.tableName) return columnTypes

            if ('tables' in recordQuery && recordQuery?.tables) {
                const newColumnType: ColumnSchemaDesc[] = []
                const columnTypeMap = new Map()
                columnTypes.forEach((columnType) => {
                    recordQuery.tables?.forEach(({ columnPrefix = '' }) => {
                        if (columnType.name.startsWith(columnPrefix)) {
                            if (columnTypeMap.has(columnType.name.replace(columnPrefix, ''))) return
                            columnTypeMap.set(columnType.name.replace(columnPrefix, ''), columnType)
                            newColumnType.push({
                                ...columnType,
                                name: columnType.name.replace(columnPrefix, ''),
                            })
                        }
                    })
                })
                return newColumnType
            }
            return []
        }, [columnTypes, recordQuery]),
    }
}

export default useFetchDatastoreByTable
