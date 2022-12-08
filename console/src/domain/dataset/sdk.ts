import { tableDataLink } from '../datastore/utils'

export type IBBox = [x: number, y: number, width: number, height: number]
export type IShape = [height: number, width: number, channels: number]
export enum MIMES {
    PNG = 'image/png',
    GRAYSCALE = 'x/grayscale',
    AUDIOWAV = 'audio/wav',
    TEXTPLAIN = 'text/plain',
}
export enum TYPES {
    COCO = 'coco_object_annotation',
    IMAGE = 'image',
    AUDIO = 'audio',
    TEXT = 'text',
    LINK = 'link',
    VIDEO = 'video',
}

export type IDatasetImage = {
    id?: number
    width: number
    height: number
    name: string
    channels: number
}

export type IObjectImage = {
    display_name: string
    mime_type: MIMES[keyof MIMES]
    _raw_base64_data: string
    shape: IShape
    encoding: string
    type: TYPES[keyof TYPES]
    as_mask: boolean
    data_type: any
}

export type IAnnotation = {
    type: 'coco_object_annotation' | 'image'
}

export type IAnnotationCOCOObject = {
    id: number
    image_id: number
    category_id: number
    // RLE or [polygon],
    _segmentation_rle_size?: [height: number, width: number]
    type: 'coco_object_annotation'
    bbox: IBBox
    iscrowd: 0 | 1
    area: number
}

export class DatasetObject {
    // raw data
    public size: number

    public offset: number

    public id: string

    public uri: string

    public src: string

    public mimeType: MIMES[keyof MIMES] | string

    public type: TYPES[keyof TYPES] | string

    public data: IObjectImage

    // annotations
    public cocos: IAnnotationCOCOObject[]

    public masks: IObjectImage[]

    public summary: Record<string, any>

    public objects: any[]

    constructor(data: any) {
        this.src = data?.src ?? 0
        this.size = Number(data?.data_size ?? 0)
        this.offset = Number(data?.data_offset ?? 0)
        this.uri = data?.data_link?.uri ?? ''
        this.id = data?.id ?? ''
        this.mimeType = ''
        this.type = ''
        this.data = {} as any
        this.summary = {}
        this.cocos = []
        this.masks = []
        this.objects = []

        // @ts-ignore
        Object.entries(data).forEach(([key, value]: [string, any]) => {
            if (key !== 'annotations') return

            try {
                Object.entries(value).forEach(([k, v]: [string, any]) => {
                    if (typeof v === 'number' || typeof v === 'string' || typeof v === 'boolean') {
                        this.summary[k] = v
                    }
                })
                value.annotations?.forEach((item?: any) => this.setProps(item))
                this.setProps(value.mask)
            } catch (e) {
                // eslint-disable-next-line no-console
                console.error(e)
            }
        })

        try {
            const json = JSON.parse(data?.data_type, undefined)
            if (json.type === TYPES.LINK) {
                this.data = json.data_type
            } else {
                this.data = json
            }
            this.mimeType = (this.data?.mime_type ?? '') as MIMES
            this.type = (this.data?.type ?? '') as MIMES
        } catch (e) {
            // eslint-disable-next-line no-console
            console.error(e)
        }
    }

    setProps(anno: any) {
        if (anno?._type === TYPES.COCO) {
            this.cocos?.push(anno)
        } else if ((anno?._type === TYPES.IMAGE || anno?._type === TYPES.LINK) && anno?.data_type?.as_mask) {
            this.masks?.push(anno)
        } else if (!anno?._type) {
            this.objects?.push(anno)
        }
    }

    getCOCOCategories(): number[] {
        return Array.from(new Set(this.cocos?.map((v: IAnnotationCOCOObject) => v.category_id)))
    }

    setDataSrc(projectId: string, datasetVersionName: string, datasetVersionVersionName: string, token: string) {
        const src = tableDataLink(projectId, datasetVersionName, datasetVersionVersionName, {
            uri: this.uri,
            offset: this.offset,
            size: this.size,
            Authorization: token as string,
        })
        this.src = src
        this.masks.forEach((msk: any) => {
            let mskUri = msk.uri
            if (msk.with_local_fs_data === 'true') {
                mskUri = msk._local_fs_uri
            }
            // eslint-disable-next-line no-param-reassign
            msk._raw_base64_data = tableDataLink(projectId, datasetVersionName, datasetVersionVersionName, {
                uri: mskUri,
                offset: msk.offset,
                size: msk.size,
                Authorization: token as string,
            })
        })
        return src
    }
}
