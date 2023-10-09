import { RecordListVo } from '../datastore'

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
export enum AnnotationType {
    COCO = 'coco_object_annotation',
    BOUNDINGBOX = 'bounding_box',
    MASK = 'mask',
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
    _extendSrc?: string
    _extendPath?: string
    _extendType?: string
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
    _type: ArtifactType.Link
    uri: string
    offset: string
    size: string
    auth: string
    scheme: string
    with_local_fs_data: boolean
    _owner: string
}
export interface ITypeCOCOObjectAnnotation extends ITypeBase {
    _type: AnnotationType.COCO
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
    _type: AnnotationType.BOUNDINGBOX
    x: number
    y: number
    width: number
    height: number
}

export type RecordT = Record<string, any>
export type SummaryT = any
export type OptionsT = {
    showPrivate?: boolean
    showAnnotationInTable?: boolean
    showLink?: boolean
}
export type DatasetT = {
    record: RecordT
    summary: Map<string, SummaryT>
    summaryTypes: Set<string>
    columnTypes: RecordListVo['columnTypes']
}
export type DatasetsT = {
    records: DatasetT[]
}
