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
    data_type: IArtifact | null
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

export function parseData(data: any): any {
    // for root
    if (!data._type && data.data_type && data.data_link) return parseRootData(data)

    if (_.isArray(data)) return data.map((item) => parseData(item))
    else if (_.isObject(data)) {
        if ('_type' in data) {
            if (data?._type === TYPES.LINK) {
                return linkToData(data as ITypeLink)
            } else if (data?._type) {
                return data
            }
        }

        const arr = {}
        Object.entries(data).forEach(([key, value]) => {
            ;(arr as any)[key] = parseData(value)
        })
        return arr
    }
    return data
}

// @FIXME none standard data_type of root object
export function parseRootData(data: ITypeLink & { data_link?: ITypeLink }): IArtifact {
    let artifact = null

    if (typeof data.data_type === 'string') {
        try {
            const json = JSON.parse(data?.data_type, undefined)
            if (json.type === TYPES.LINK) {
                artifact = json.data_type
            } else {
                artifact = json
            }
            artifact = {
                _type: artifact.type,
                _mime_type: artifact.mime_type,
                ...artifact,
            }
            if (data.data_link) {
                artifact.link = data.data_link

                // @ts-ignore
                if (data.data_offset) artifact.link.offset = data.data_offset
                // @ts-ignore
                if (data.data_size) artifact.link.size = data.data_size
            }
        } catch (e) {}
    }

    return artifact
}

export function linkToData(data: ITypeLink & { data_link?: ITypeLink }): IArtifact {
    let artifact = null

    if (typeof data.data_type === 'string') {
        try {
            const json = JSON.parse(data?.data_type, undefined)
            if (json.type === TYPES.LINK) {
                artifact = json.data_type
            } else {
                artifact = json
            }
        } catch (e) {}
    } else if (typeof data.data_type === 'object') {
        artifact = data.data_type as IArtifact
    }

    if (data._type === TYPES.LINK) {
        artifact.link = { ...data, data_type: null }
    }

    if (artifact.link) {
        artifact.src = String(artifact.link.uri).startsWith('http') ? artifact.link.uri : null
    }

    return (artifact as IArtifact) ?? {}
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

    public annotations: Record<string, any> = {}

    //

    public cocos: ITypeCOCOObjectAnnotation[] = []

    public masks: IArtifactImage[] = []

    public bboxes: ITypeBoundingBox[] = []

    constructor(data: any, curryParseLinkFn: any) {
        this.size = data.data_size
        this.id = data.id

        this.data = {} as any
        this.summary = {}

        // @ts-ignore
        Object.entries(data).forEach(([key, value]: [string, any]) => {
            if (!key.startsWith('annotation/')) return
            if (typeof value === 'number' || typeof value === 'string' || typeof value === 'boolean') {
                this.summary[key] = value
                return
            }

            if (typeof value === 'object') {
                this.annotations[key] = parseData(value)
                this.parseAnnotations()
            }
        })

        this.data = parseData(data)
        if (this.data.link && !this.data.src) this.data.src = curryParseLinkFn(this.data.link)
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
                anno._path = [...path].join('.')

                if ((anno as ITypeCOCOObjectAnnotation)._type === TYPES.COCO) {
                    cocos?.push(anno as any)
                } else if ((anno as ITypeBoundingBox)._type === TYPES.BOUNDINGBOX) {
                    bboxes?.push(anno as any)
                } else if ((anno as IArtifactImage)._type === ArtifactType.Image && (anno as IArtifactImage)?.as_mask) {
                    masks?.push(anno as any)
                }

                Object.entries(anno).forEach(([key, anno]: any) => {
                    find(anno, [...path, key])
                })
            }
        }
        find(this.annotations)
        this.cocos = cocos
        this.masks = masks
        this.bboxes = bboxes
    }

    getCOCOCategories(): number[] {
        return Array.from(new Set(this.cocos?.map((v: ITypeCOCOObjectAnnotation) => v.category_id)))
    }
}
