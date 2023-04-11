import React from 'react'
import { ColumnSchemaDesc } from '../schemas/datastore'
import { DataTypes } from '../../datastore'
import { SwType } from '../model'
import _ from 'lodash'

export type RecordListSchemaT = Record<string, RecordSchemaT>[]
export type RecordSchemaT = {
    type: DataTypes
    name: string
    value: any
    mixed: boolean
}

export function useDatastoreWithSchema(records: RecordListSchemaT, columnTypes: ColumnSchemaDesc[]) {
    const getSchema = React.useCallback(
        (name: string, rowIndex: number = 0) => {
            return columnTypes.find((c) => c.name === name)
        },
        [columnTypes]
    )

    return {
        records,
        columnTypes,
        getSchema,
    }
}

export function useDatastore(records: RecordListSchemaT = []) {
    const cached = React.useRef(new LRUCache<string, any>(1000))

    const getSchema = React.useCallback(
        (name: string, rowIndex: number = 0): RecordSchemaT | undefined => {
            const key = `${rowIndex}.${name}`
            if (cached.current.has(key)) return cached.current.get(key)
            const recordTmp = _.get(records, key)
            if (!recordTmp) return undefined
            let schema = SwType.decode_schema(recordTmp)
            if (schema) {
                schema = {
                    ...schema,
                    name: name,
                    mixed: true,
                }
                cached.current.put(key, schema)
            }
            return schema
        },
        [records]
    )

    const $records = React.useMemo(() => {
        if (records.length === 0) return []

        return records.map((record, index) => {
            const recordTmp: Record<string, any> = {}
            Object.keys(record).forEach((key) => {
                const schema = getSchema(key, index)
                if (schema) recordTmp[key] = schema
            })
            return recordTmp
        })
    }, [records, getSchema])

    const $columnTypes = React.useMemo(() => {
        if (records.length === 0) return []

        const columnTypes: RecordSchemaT[] = []
        const rowIndex = 0
        const record = records[rowIndex]
        Object.keys(record).forEach((key) => {
            const schema = getSchema(key, rowIndex)
            if (schema) columnTypes.push(schema)
        })
        return columnTypes
    }, [records, getSchema])

    return {
        records: $records,
        columnTypes: $columnTypes,
        getSchema,
    }
}

export default useDatastore

class LRUCache<Key, Value> {
    private maxSize: number
    private cache: Map<Key, Value>
    private accessedKeys: Set<Key>

    constructor(maxSize: number) {
        this.maxSize = maxSize
        this.cache = new Map<Key, Value>()
        this.accessedKeys = new Set<Key>()
    }

    has(key: Key): boolean {
        return this.cache.has(key)
    }

    get(key: Key): Value | undefined {
        if (this.cache.has(key)) {
            // Move the key to the front of the accessedKeys set
            this.accessedKeys.delete(key)
            this.accessedKeys.add(key)
            return this.cache.get(key)
        } else {
            return undefined
        }
    }

    put(key: Key, value: Value) {
        if (this.cache.has(key)) {
            // Move the key to the front of the accessedKeys set
            this.accessedKeys.delete(key)
            this.accessedKeys.add(key)
            this.cache.set(key, value)
        } else {
            if (this.cache.size >= this.maxSize) {
                // Remove the least recently used key from the cache and accessedKeys set
                const lruKey = this.accessedKeys.values().next().value
                this.accessedKeys.delete(lruKey)
                this.cache.delete(lruKey)
            }
            // Add the new key to the front of the accessedKeys set
            this.accessedKeys.add(key)
            this.cache.set(key, value)
        }
    }
}
