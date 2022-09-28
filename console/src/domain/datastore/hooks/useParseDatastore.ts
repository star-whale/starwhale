import keyBy from 'lodash/keyBy'
import React from 'react'
import { RecordListVO } from '../schemas/datastore'

export function useParseConfusionMatrix(data: RecordListVO = {}) {
    const labels = React.useMemo(() => {
        const { columnTypes } = data
        return (columnTypes?.map((column) => column.name)?.filter((name) => name !== 'id') ?? []).sort()
    }, [data])

    const binarylabel = React.useMemo(() => {
        const { records = [] } = data
        const recordMap = keyBy(records, 'id')
        const rtn: any[][] = []
        labels.forEach((labeli, i) => {
            labels.forEach((labelj, j) => {
                if (!rtn[i]) rtn[i] = []
                // Typer?.[columnTypes?.[labelj]]?.encode
                rtn[i][j] = recordMap?.[labeli]?.[labelj] ?? '' ?? ''
            })
        })
        return rtn
    }, [data, labels])

    return { labels, binarylabel }
}

export function useParseRocAuc(data: RecordListVO = {}) {
    const rocAuc = React.useMemo(() => {
        const { records = [] } = data
        const fpr: number[] = []
        const tpr: number[] = []
        records.sort((a, b) => {
            return parseInt(a.id, 10) - parseInt(b.id, 10)
        })
        records.forEach((item, i) => {
            if (i > 20 && i % 20 !== 0) return

            fpr.push(Number(item.fpr))
            tpr.push(Number(item.tpr))
        })
        return {
            records,
            fpr,
            tpr,
        }
    }, [data])

    return rocAuc
}
