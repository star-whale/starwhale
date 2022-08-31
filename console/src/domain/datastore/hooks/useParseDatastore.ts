import omit from 'lodash/omit'
import keyBy from 'lodash/keyBy'
import React from 'react'
import { RecordListVO } from '../schemas/datastore'
import { unhexlify } from '../utils'

export function useParseConfusionMatrix(data: RecordListVO = {}) {
    const labels = React.useMemo(() => {
        const { columnTypes } = data
        return Object.keys(omit(columnTypes, 'id')).sort()
    }, [data])

    const binarylabel = React.useMemo(() => {
        const { records = [] } = data
        const recordMap = keyBy(records, 'id')
        const rtn: any[][] = []
        labels.forEach((labeli, i) => {
            labels.forEach((labelj, j) => {
                if (!rtn[i]) rtn[i] = []
                rtn[i][j] = unhexlify(recordMap?.[labeli]?.[labelj] ?? '') ?? ''
            })
        })
        return rtn
    }, [data, labels])

    // console.log(labels, binarylabel)
    return { labels, binarylabel }
}
