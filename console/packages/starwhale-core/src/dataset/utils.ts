import { tableDataLink } from '@starwhale/core/datastore'
import _ from 'lodash'
import { AnnotationType, ArtifactType, ITypeLink, OptionsT, RecordT, SummaryT } from './types'

export const parseDataSrc = _.curry((token: string, link: ITypeLink) => {
    const { uri, offset, size } = link ?? {}
    const src = tableDataLink({
        uri,
        offset,
        size,
        Authorization: token as string,
    })
    return src
})

export const isPrivate = (str: string) => str.startsWith('sys/_') || str.startsWith('_')
export const isLink = (data: any) => typeof data === 'object' && data?._type && data?._type === ArtifactType.Link
export const isMask = (data: any) =>
    typeof data === 'object' && data?._type && data?._type === ArtifactType.Image && data?.as_mask === 'true'
export const isAnnotationType = (type: string) => {
    // eslint-disable-next-line
    for (let t in AnnotationType) {
        // @ts-ignore
        if (AnnotationType[t] === type) return true
    }
    return false
}
export const isAnnotation = (data: any) => (typeof data === 'object' && isAnnotationType(data?._type)) || isMask(data)
export const isAnnotationHiddenInTable = (data: any) => isAnnotation(data) && !isMask(data)

export function linkToData(data: ITypeLink, curryParseLinkFn: any): string {
    if (curryParseLinkFn) return curryParseLinkFn(data)
    if (data.uri?.startsWith('http')) {
        return data.uri
    }
    const token = (window.localStorage && window.localStorage.getItem('token')) ?? ''
    return parseDataSrc(token, data)
}

// @FIXME add cache
export function getSummary(record: RecordT, options: OptionsT) {
    const summaryTmp = new Map<string, SummaryT>()
    const summaryTypesTmp = new Set<string>()

    Object.entries(record).forEach(([key, value]: [string, any]) => {
        if (!options.showPrivate && isPrivate(key)) return
        function flatObjectWithPaths(anno: any, path: any[] = []) {
            // with all fields
            summaryTmp.set([...path].join('.'), anno)

            if (_.isArray(anno)) {
                anno.forEach((item: any, index: number) => flatObjectWithPaths(item, [...path, index]))
            } else if (_.isPlainObject(anno)) {
                // without link
                if (!options.showLink && isLink(anno)) return

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
