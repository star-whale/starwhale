// @ts-nocheck
/* eslint-disable */
import omit from 'lodash/omit'
import keyBy from 'lodash/keyBy'
import React from 'react'
import { RecordListVO } from '../schemas/datastore'
import struct from '@aksel/structjs'

const unhexlify = function (str) {
    const f = new Uint8Array(8)
    let j = 0
    for (var i = 0, l = str.length; i < l; i += 2) {
        f[j] = parseInt(str.substr(i, 2), 16)
        j++
    }
    let s = struct('>d')

    return s.unpack(f.buffer)[0]
}
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
                rtn[i][j] = unhexlify(recordMap?.[labeli.split('_')[1]]?.[labelj]) ?? ''
            })
        })
        return rtn
    }, [data, labels])

    // console.log(labels, binarylabel)
    return { labels, binarylabel }
}
