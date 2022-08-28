import React from 'react'
import ImageViewer from './ImageViewer'

export type IDatasetViewerProps = {
    data: IImageProps
}
export type IImageProps = {
    type: 'image'
    label: string
    name: string
    src: string
}

export default function DatasetViewer({ data }: IDatasetViewerProps) {
    return <ImageViewer data={data} />
}
