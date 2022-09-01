import React from 'react'
import IconFont from '@/components/IconFont'
import ImageGrayscaleViewer from './ImageGrayscaleViewer'

export enum MIME {
    GRAYSCALE = 'x/grayscale',
}

export type IDatasetViewerProps = {
    data: IDatasetMeta
}

export type IDatasetMeta = {
    type: MIME[keyof MIME]
    label: string
    name: string
    src: string
}

export default function DatasetViewer({ data }: IDatasetViewerProps) {
    const { type } = data

    // eslint-disable-next-line default-case
    switch (type) {
        case MIME.GRAYSCALE:
            return <ImageGrayscaleViewer data={data} />
    }
    return (
        <p
            style={{
                height: '68px',
                width: '100px',
                display: 'grid',
                placeItems: 'center',
                borderRadius: '3px',
                backgroundColor: '#F7F8FA',
                color: 'rgba(2,16,43,0.20)',
                border: '1px solid #E2E7F0',
            }}
        >
            <IconFont type='excel' size={28} />
        </p>
    )
}
