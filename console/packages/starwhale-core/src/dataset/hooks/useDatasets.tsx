import React from 'react'
import { AnnotationType, ArtifactType, ITypeLink } from '../types'
import _ from 'lodash'
import { RecordListVO } from '@starwhale/core/datastore/schemas/datastore'
import { tableDataLink } from '@starwhale/core/datastore'

export type RecordT = Record<string, any>
export type SummaryT = any
export type OptionsT = {
    showPrivate?: boolean
    showLink?: boolean
    parseLink?: () => any
}
export type DatasetT = {
    record: RecordT
    summary: Map<string, SummaryT>
    summaryTypes: Set<string>
    columnTypes: RecordListVO['columnTypes']
}
export type DatasetsT = {
    records: DatasetT[]
}

export const parseDataSrc = _.curry(
    (
        projectId: string,
        datasetVersionName: string,
        datasetVersionVersionName: string,
        token: string,
        link: ITypeLink
    ) => {
        const { uri, offset, size } = link ?? {}
        const src = tableDataLink(projectId, datasetVersionName, datasetVersionVersionName, {
            uri,
            offset,
            size,
            Authorization: token as string,
        })
        return src
    }
)
const isPrivate = (str: string) => str.startsWith('sys/_') || str.startsWith('_')
const isLink = (data: any) => typeof data === 'object' && data?._type && data?._type === ArtifactType.Link
const isMask = (data: any) =>
    typeof data === 'object' && data?._type && data?._type === ArtifactType.Image && data?.as_mask === 'true'
const isAnnotationType = (type: string) => {
    // eslint-disable-next-line
    for (let t in AnnotationType) {
        // @ts-ignore
        if (AnnotationType[t] === type) return true
    }
    return false
}

const isAnnotation = (data: any) => (typeof data === 'object' && isAnnotationType(data?._type)) || isMask(data)

export function linkToData(data: ITypeLink, curryParseLinkFn: any): string {
    return data.uri.startsWith('http') ? data.uri : curryParseLinkFn(data)
}

export function getSummary(record: RecordT, options: OptionsT) {
    const summaryTmp = new Map<string, SummaryT>()
    const summaryTypesTmp = new Set<string>()

    Object.entries(record).forEach(([key, value]: [string, any]) => {
        // if (!options.showPrivate && isPrivate(key)) return
        function flatObjectWithPaths(anno: any, path: any[] = []) {
            if (_.isArray(anno)) {
                anno.forEach((item: any, index: number) => flatObjectWithPaths(item, [...path, index]))
            } else if (_.isPlainObject(anno)) {
                // without link
                // if (!options.showLink && anno._type === 'link') return

                // add path & src
                if (anno._type) {
                    summaryTypesTmp.add(isMask(anno) ? AnnotationType.MASK : anno._type)
                    summaryTmp.set([...path].join('.'), {
                        ...anno,
                        _extendPath: [...path].join('.'),
                        _extendSrc: anno.link ? linkToData(anno.link, options.parseLink) : undefined,
                        _extendType: isMask(anno) ? AnnotationType.MASK : anno._type,
                    })
                }

                Object.entries(anno).forEach(([_key, tmp]: any) => {
                    if (_.isPlainObject(tmp) || _.isArray(anno)) {
                        flatObjectWithPaths(tmp, [...path, _key])
                    }
                })
            } else {
                summaryTmp.set([...path].join('.'), anno)
            }
        }
        flatObjectWithPaths(value, [key])
    })

    return {
        record,
        summary: summaryTmp,
        summaryTypes: summaryTypesTmp,
    }
}

export function useDatasetTableColumns(summary: SummaryT, options: OptionsT) {
    const summaryForTable = React.useMemo(() => {
        const m = new Map()
        summary.forEach((value: any, key: string) => {
            if (!options.showPrivate && isPrivate(key)) return
            if (!options.showLink && isLink(value)) return
            if (!isAnnotation(value)) return

            m.set(key, value)
        })
        return m
    }, [summary, options])

    return {
        summaryForTable,
    }
}

export function useDatasetTableAnnotations(dataset: DatasetT, options?: OptionsT) {
    const annotations = React.useMemo(() => {
        const tmp = new Map()
        dataset.summary.forEach((value: any, key: string) => {
            if (!options?.showPrivate && isPrivate(key)) return
            if (!options?.showLink && isLink(value)) return
            if (!isAnnotation(value)) return
            tmp.set(key, value)
        })
        return tmp
    }, [dataset, options])

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

export function useDatasets<T extends RecordListVO['records'] = RecordT[]>(
    records: T,
    columnTypes: RecordListVO['columnTypes'],
    options: OptionsT
): DatasetsT {
    const recordsTmp = React.useMemo(() => {
        return (
            records?.map((record) => {
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
