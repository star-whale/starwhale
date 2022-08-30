import omit from 'lodash/omit'
import keyBy from 'lodash/keyBy'
import set from 'lodash/set'
import React from 'react'
import { RecordListVO } from '../schemas/datastore'

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
                rtn[i][j] = parseFloat(recordMap?.[labeli.split('_')[1]]?.[labelj]) ?? ''
            })
        })
        return rtn
    }, [data, labels])

    console.log(labels, binarylabel)
    return { labels, binarylabel }
}
