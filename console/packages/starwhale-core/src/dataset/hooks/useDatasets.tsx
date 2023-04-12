import React from 'react'
import { RecordT, OptionsT, DatasetsT } from '../types'
import { RecordListVO } from '@starwhale/core/datastore/schemas/datastore'
import { getSummary } from '../utils'

export function useDatasets<T extends RecordListVO['records'] = RecordT[]>(
    records: T,
    columnTypes: RecordListVO['columnTypes'],
    options: OptionsT
): DatasetsT {
    const recordsTmp = React.useMemo(() => {
        return (
            records
                ?.map((record) => {
                    const tmp: Record<string, any> = {}
                    Object.entries(record).forEach(([key, v]) => {
                        tmp[key] = v.value
                    })
                    return tmp
                })
                .map((record) => {
                    return {
                        ...getSummary(record, options),
                        columnTypes,
                    }
                }) ?? []
        )
    }, [records, options, columnTypes])

    return {
        records: recordsTmp,
    }
}
