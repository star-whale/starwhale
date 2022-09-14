import isObject from 'lodash/isObject'
import { IDataType } from '../datastore/sdk'
import Typer from '../datastore/sdk'
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
    MASK = 'mask',
    IMAGE = 'image',
    AUDIO = 'audio',
    TEXT = 'text',
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
}

export type IAnnotation = {
    type: 'coco_object_annotation' | 'image'
}

export type IAnnotationCOCOObject = {
    id: number
    image_id: number
    category_id: number
    // RLE or [polygon],
    segmentation?: {
        size: [height: number, width: number]
    }
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
    public authName: string
    public src: string
    public mimeType: MIMES[keyof MIMES] | string
    public type: TYPES[keyof TYPES] | string
    public data: IObjectImage
    public columnTypes: Record<string, IDataType>
    // annotations
    public cocos: IAnnotationCOCOObject[]
    public masks: IObjectImage[]
    public summary: Record<string, any>
    public objects: any[]

    constructor(data: any, columnTypes: any) {
        this.src = data?.src ?? 0
        this.size = data?.data_size ?? 0
        this.offset = data?.data_offset ?? 0
        this.uri = data?.data_uri ?? ''
        this.authName = data?.auth_name ?? ''
        this.id = data?.id ?? ''
        this.columnTypes = columnTypes
        this.mimeType = ''
        this.type = ''
        this.data = {} as any
        this.summary = {}
        this.cocos = []
        this.masks = []
        this.objects = []

        // @ts-ignore
        Object.entries(data).forEach(([key, value]: [string, string], i: number) => {
            if (!key.startsWith('_annotation')) return
            const attr = key.replace(/^_annotation?_/, '')

            try {
                const annos = JSON.parse(value, undefined)
                if (Array.isArray(annos)) {
                    annos.forEach((item) => this.setProps(item))
                } else if (isObject(annos)) {
                    this.setProps(annos)
                } else {
                    this.summary[attr] = annos
                }
            } catch (e) {
                console.log(e)
            }
        })

        try {
            this.data = JSON.parse(data?.data_type, undefined)
            this.mimeType = (this.data?.mime_type ?? '') as MIMES
            this.type = (this.data?.type ?? '') as MIMES
        } catch (e) {
            console.log(e)
        }
    }
    setProps(anno: any) {
        if (anno?.type === TYPES.COCO) {
            this.cocos?.push(anno)
        } else if (anno?.type === TYPES.IMAGE) {
            this.masks?.push(anno)
        } else if (!anno?.type) {
            this.objects?.push(anno)
        }
    }
    getDatastoreLink() {}
    getCOCOCategories(): number[] {
        return Array.from(new Set(this.cocos?.map((v: IAnnotationCOCOObject) => v.category_id)))
    }
    setDataSrc(projectId: string, datasetVersionName: string, datasetVersionVersionName: string, token: string) {
        const src = tableDataLink(projectId, datasetVersionName, datasetVersionVersionName, {
            uri: this.uri,
            authName: this.authName,
            offset: Typer?.[this.columnTypes.data_offset]?.encode(this.offset),
            size: Typer?.[this.columnTypes.data_size]?.encode(this.size),
            Authorization: token as string,
        })
        this.src = src
        return src
    }
}
