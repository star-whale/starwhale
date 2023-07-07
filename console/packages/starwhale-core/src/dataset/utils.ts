import { tableDataLink } from '@starwhale/core/datastore'
import _ from 'lodash'
import { AnnotationType, ArtifactType, ITypeLink, OptionsT, RecordT, SummaryT } from './types'

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

// from: _owner "http://controller:8082/project/starwhale/dataset/pfp/version/yqpjypceemtxqhk45oy5wjuyfnf3z44gv5pez5tr"
// to: e2e-20230516/controller/project/1/common-dataset/pfp/03/032e234c996e43a4dbfa1a7936864cc59bafa6465ce1ff6a4a10935c0defb84c375ecc6d32ea78a3ab4451fbed05057899dc66bc2cce1824c8eebc0ce240d7fa
export function linkWithOwner(data: ITypeLink): string {
    const matches = data._owner.match(/project\/(.*?)\/dataset\/(.*?)\/version\/(.*)/)
    const [, projectName, datasetName, datasetVersionName] = matches ?? []
    const token = (window.localStorage && window.localStorage.getItem('token')) ?? ''
    return parseDataSrc(projectName, datasetName, datasetVersionName, token, data)
}
export function linkToData(data: ITypeLink, curryParseLinkFn: any): string {
    if (data._owner) return linkWithOwner(data)
    if (data.uri?.startsWith('http')) return data.uri
    if (curryParseLinkFn) return curryParseLinkFn(data)
    return data.uri
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
