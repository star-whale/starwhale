import { isNaN } from 'lodash'
import keyBy from 'lodash/keyBy'
import React from 'react'
import { RecordListVo } from '../schemas/datastore'

export function useParseConfusionMatrix(data: RecordListVo = {}) {
    const labels = React.useMemo(() => {
        const { columnTypes } = data
        const columnArr = columnTypes?.map((column) => column.name)?.filter((name) => name !== 'id') ?? []
        return columnArr.sort((a, b) => {
            if (!isNaN(Number(a))) {
                return Number(a) > Number(b) ? 1 : -1
            }
            if (!a || !b) return 1

            return a > b ? 1 : -1
        })
    }, [data])

    const binarylabel = React.useMemo(() => {
        const { records = [] } = data
        const recordMap = keyBy(records, 'id')
        const rtn: any[][] = []
        labels.forEach((labeli, i) => {
            labels.forEach((labelj, j) => {
                if (!rtn[i]) rtn[i] = []
                // Typer?.[columnTypes?.[labelj]]?.encode
                // @ts-ignore
                rtn[i][j] = recordMap?.[labeli]?.[labelj] ?? '' ?? ''
            })
        })
        return rtn
    }, [data, labels])

    return { labels, binarylabel }
}

export function useParseRocAuc(
    data: {
        records: Record<string, any>
        x?: string
        y?: string
    } = { records: [] }
) {
    const rocAuc = React.useMemo(() => {
        const { records, x, y } = data
        const fpr: number[] = []
        const tpr: number[] = []
        records.sort((a: any, b: any) => {
            return parseInt(a?.id, 10) - parseInt(b?.id, 10)
        })
        records.forEach((item: any, i: number) => {
            if (i > 20 && i % 20 !== 0) return

            const xnum = item?.[x ?? 'fpr']
            const ynum = item?.[y ?? 'tpr']
            if (xnum && !Number.isNaN(xnum)) fpr.push(Number(xnum))
            if (ynum && !Number.isNaN(ynum)) tpr.push(Number(ynum))
        })
        return {
            records,
            fpr,
            tpr,
        }
    }, [data])

    return rocAuc
}
