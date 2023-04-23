import React from 'react'
import { RecordT, OptionsT, DatasetsT } from '../types'
import { RecordListVo } from '@starwhale/core/datastore/schemas/datastore'
import { getSummary } from '../utils'

function val(r: any) {
    if (r === undefined) return ''
    if (typeof r === 'object' && 'value' in r) {
        // dataset use raw value should not be json encode
        return r.value
    }

    return r
}

export function useDatasets<T extends RecordListVo['records'] = RecordT[]>(
    records: T,
    columnTypes: RecordListVo['columnTypes'],
    options: OptionsT
): DatasetsT {
    const recordsTmp = React.useMemo(() => {
        return (
            records
                ?.map((record) => {
                    const tmp: Record<string, any> = {}
                    Object.entries(record).forEach(([key, v]) => {
                        tmp[key] = val(v)
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
