import { tableDataLink } from '@starwhale/core/datastore'
import _ from 'lodash'
import { RecordListVO } from '@starwhale/core/datastore/schemas/datastore'
import { ArtifactType, IArtifactImage, ITypeBoundingBox, ITypeCOCOObjectAnnotation, ITypeLink } from './types'

export function linkToData(data: ITypeLink, curryParseLinkFn: any): string {
    return data.uri.startsWith('http') ? data.uri : curryParseLinkFn(data)
}

export function parseData(data: any, curryParseLinkFn: any): any {
    // for root

    if (_.isArray(data)) {
        return data.map((item) => parseData(item, curryParseLinkFn))
    }

    if (_.isObject(data)) {
        if ('_type' in data) {
            // @ts-ignore
            if (data?._type && data.link) {
                // @ts-ignore
                // eslint-disable-next-line no-param-reassign
                data.src = linkToData(data.link, curryParseLinkFn)
                return data
            }
        }

        const arr = {}
        Object.entries(data).forEach(([key, value]) => {
            ;(arr as any)[key] = parseData(value, curryParseLinkFn)
        })
        return arr
    }
    return data
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

// exclucdeProperties
// - showPrivate
// - showLink
// findProperties

function AnnotationMask(options) {
    const { data } = options
    return {
        is: typeof data === 'object' && data?._type && data?._type === ArtifactType.Image && data?.as_mask !== 'true',
        // render:
    }
}

export class DatasetObject {
    public _data: RecordListVO

    /**
     * used for table list
     */
    public summary: Record<string, any>

    public cocos: ITypeCOCOObjectAnnotation[] = []

    public cocoCats: number[] = []

    public masks: IArtifactImage[] = []

    public bboxes: ITypeBoundingBox[] = []

    constructor(data: RecordListVO, curryParseLinkFn: any) {
        this._data = data
        this.summary = {}

        return

        // @ts-ignore
        Object.entries(data).forEach(([key, value]: [string, any]) => {
            if (key === 'id') {
                this.id = value
                return
            }
            if (!key.startsWith('data/')) return
            const keyShow = key
            if (typeof value === 'number' || typeof value === 'string' || typeof value === 'boolean') {
                this.summary[keyShow] = value
                return
            }

            if (typeof value === 'object') {
                const parseData1 = parseData(value, curryParseLinkFn)
                if (parseData1?._type) {
                    this.summary[keyShow] = parseData1
                } else {
                    this.summary[`_${keyShow}`] = parseData1
                }
            }
        })
        this.parseAnnotations()
        Object.entries(this.summary).forEach(([, value]: [string, any]) => {
            if (
                typeof value === 'object' &&
                value?._type &&
                value?._type === ArtifactType.Image &&
                value?.as_mask !== 'true'
            ) {
                // eslint-disable-next-line no-param-reassign
                value.cocos = this.cocos
                // eslint-disable-next-line no-param-reassign
                value.cocoCats = this.cocoCats
                // eslint-disable-next-line no-param-reassign
                value.masks = this.masks
                // eslint-disable-next-line no-param-reassign
                value.bboxes = this.bboxes
            }
        })
    }

    parseAnnotations() {
        const cocos: ITypeCOCOObjectAnnotation[] = []
        const masks: IArtifactImage[] = []
        const bboxes: ITypeBoundingBox[] = []

        function find(anno: any, path: any[] = []) {
            if (_.isArray(anno)) {
                anno.forEach((item: any, index: number) => find(item, [...path, index]))
            } else if (_.isObject(anno)) {
                // @ts-ignore
                // eslint-disable-next-line
                anno._path = [...path].join('.')

                if ((anno as ITypeCOCOObjectAnnotation)._type === TYPES.COCO) {
                    cocos?.push(anno as any)
                } else if ((anno as ITypeBoundingBox)._type === TYPES.BOUNDINGBOX) {
                    bboxes?.push(anno as any)
                } else {
                    // @ts-ignore
                    // eslint-disable-next-line no-lonely-if
                    if ((anno as IArtifactImage)._type === ArtifactType.Image && anno?.as_mask === 'true') {
                        masks?.push(anno as any)
                    }
                }

                Object.entries(anno).forEach(([key, tmp]: any) => {
                    if (_.isObject(tmp) || _.isArray(anno)) {
                        find(tmp, [...path, key])
                    }
                })
            }
        }
        find(this.summary)
        this.cocos = cocos
        this.cocoCats = this.getCOCOCategories()
        this.masks = masks
        this.bboxes = bboxes
    }

    getCOCOCategories(): number[] {
        return Array.from(new Set(this.cocos?.map((v: ITypeCOCOObjectAnnotation) => v.category_id)))
    }
}
