import React from 'react'
import IconFont from '@/components/IconFont'
import { DatasetObject, MIMES, TYPES } from '@/domain/dataset/sdk'
import ImageViewer from '@/components/Viewer/ImageViewer'
import AudioViewer from './AudioViewer'
import ImageGrayscaleViewer from './ImageGrayscaleViewer'

export type IDatasetViewerProps = {
    data?: DatasetObject
    isZoom?: boolean
    // coco
    hiddenLabels?: Set<number>
}

export function Placeholder() {
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

export default function DatasetViewer({ data, isZoom = false, hiddenLabels = new Set() }: IDatasetViewerProps) {
    const { mimeType, src, type } = data ?? {}
    const Viewer = React.useMemo(() => {
        if (!data || !src) return <Placeholder />

        switch (type) {
            case TYPES.IMAGE:
                if (mimeType === MIMES.GRAYSCALE) {
                    return <ImageGrayscaleViewer data={{ src }} isZoom={isZoom} />
                }
                return (
                    <ImageViewer
                        data={{ src }}
                        cocos={data.cocos ?? []}
                        masks={data.masks}
                        isZoom={isZoom}
                        hiddenLabels={hiddenLabels}
                    />
                )
            case TYPES.AUDIO:
                return <AudioViewer data={data} isZoom={isZoom} />
            case TYPES.TEXT:
                return <p>{data?.data?.display_name}</p>
            default:
                return <Placeholder />
        }
    }, [data, src, type, mimeType, hiddenLabels, isZoom])

    return Viewer
}
