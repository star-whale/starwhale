import React from 'react'
import { DatasetT, OptionsT } from '../types'
import { isAnnotation, isAnnotationType } from '../utils'

// eslint-disable-next-line @typescript-eslint/no-unused-vars
export function useDatasetTableAnnotations(dataset: DatasetT, options?: OptionsT) {
    const annotations = React.useMemo(() => {
        const tmp = new Map()
        dataset.summary.forEach((value: any, key: string) => {
            if (!isAnnotation(value)) return
            tmp.set(key, value)
        })
        return tmp
    }, [dataset])

    const annotationTypes = React.useMemo(() => {
        return new Set(Array.from(dataset.summaryTypes).filter(isAnnotationType))
    }, [dataset])

    const annotationTypeMap = React.useMemo(() => {
        const m = new Map()
        annotations.forEach((value: any, key: any) => {
            if (!m.has(value._extendType)) m.set(value._extendType, [])
            m.get(value._extendType).push(key)
        })
        return m
    }, [annotations])

    return {
        annotations,
        annotationTypes,
        annotationTypeMap,
    }
}
