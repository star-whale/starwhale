import React from 'react'
import { RecordListSchemaT } from '../types'
import { SwType } from '../model'

export function decordRecords(records: RecordListSchemaT) {
    if (!records || records.length === 0) return []

    return records?.map((record) => {
        const recordTmp: Record<string, any> = {}
        Object.keys(record).forEach((key) => {
            const schema = SwType.decode_schema(record[key], true)
            if (schema) recordTmp[key] = schema.value
        })
        return recordTmp
    })
}

export function useDatastoreDecodeRecords(records: RecordListSchemaT) {
    const $records = React.useMemo(() => {
        if (!records || records.length === 0) return []

        return records?.map((record) => {
            const recordTmp: Record<string, any> = {}
            Object.keys(record).forEach((key) => {
                const schema = SwType.decode_schema(record[key], true)
                if (schema) recordTmp[key] = schema.value
            })
            return recordTmp
        })
    }, [records])

    return $records
}
