import { tableDataLink } from '@starwhale/core/datastore'
import _ from 'lodash'

export type IBBox = [x: number, y: number, width: number, height: number]
export type IShape = [height: number, width: number, channels: number]
export enum MIMES {
    PNG = 'image/png',
    GRAYSCALE = 'x/grayscale',
    AUDIOWAV = 'audio/wav',
    TEXTPLAIN = 'text/plain',
    JPEG = 'image/jpeg',
    SVG = 'image/svg+xml',
    GIF = 'image/gif',
    APNG = 'image/apng',
    AVIF = 'image/avif',
    MP4 = 'video/mp4',
    AVI = 'video/avi',
    WEBM = 'video/webm',
    WAV = 'audio/wav',
    MP3 = 'audio/mp3',
    PLAIN = 'text/plain',
    CSV = 'text/csv',
    HTML = 'text/html',
    UNDEFINED = 'x/undefined',
}
export enum ArtifactType {
    Binary = 'binary',
    Image = 'image',
    Video = 'video',
    Audio = 'audio',
    Text = 'text',
    Link = 'link',
}
export enum TYPES {
    COCO = 'coco_object_annotation',
    IMAGE = 'image',
    AUDIO = 'audio',
    TEXT = 'text',
    LINK = 'link',
    VIDEO = 'video',
    BOUNDINGBOX = 'bounding_box',
}

// artifacts
export type IArtifact = {
    fp: string
    display_name: string
    shape: IShape
    encoding: string
    _mime_type: MIMES
    _type: ArtifactType
    _dtype_name: string
    // extends
    link: ITypeLink
    src?: string
    _path?: string
}

export interface IArtifactImage extends IArtifact {
    _type: ArtifactType.Image
    _mime_type: MIMES.PNG | MIMES.JPEG | MIMES.SVG | MIMES.GIF | MIMES.APNG
    as_mask: boolean
    mask_uri: string
}
export interface IArtifactVideo extends IArtifact {
    _type: ArtifactType.Video
    _mime_type: MIMES.MP4 | MIMES.AVI | MIMES.WEBM
}
export interface IArtifactAudio extends IArtifact {
    _type: ArtifactType.Audio
    _mime_type: MIMES.MP3 | MIMES.WAV
}
export interface IArtifactBinary extends IArtifact {
    _type: ArtifactType.Binary
    _mime_type: MIMES.UNDEFINED
}
export interface IArtifactText extends IArtifact {
    _type: ArtifactType.Text
    _mime_type: MIMES.PLAIN
}

// annotation types
export type ITypeBase = {
    _path?: string
}
export interface ITypeLink extends ITypeBase {
    _type: TYPES.LINK
    uri: string
    offset: string
    size: string
    auth: string
    scheme: string
    with_local_fs_data: boolean
}

export interface ITypeCOCOObjectAnnotation extends ITypeBase {
    _type: TYPES.COCO
    id: number
    image_id: number
    category_id: number
    bbox: IBBox
    iscrowd: 0 | 1
    area: number
    _segmentation_rle_size: IShape
    _segmentation_rle_counts: string
}

export interface ITypeBoundingBox extends ITypeBase {
    _type: TYPES.BOUNDINGBOX
    x: number
    y: number
    width: number
    height: number
}

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

export class DatasetObject {
    public size: string

    public id: string

    public data: IArtifact

    public summary: Record<string, any>

    //

    public cocos: ITypeCOCOObjectAnnotation[] = []

    public cocoCats: number[] = []

    public masks: IArtifactImage[] = []

    public bboxes: ITypeBoundingBox[] = []

    constructor(data: any, curryParseLinkFn: any) {
        this.size = data.data_size
        this.id = data.id

        this.data = {} as any
        this.summary = {}

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
