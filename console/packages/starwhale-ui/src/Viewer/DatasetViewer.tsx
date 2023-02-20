import React from 'react'
import IconFont from '@starwhale/ui/IconFont'
import {
    ArtifactType,
    IArtifactAudio,
    IArtifactImage,
    IArtifactVideo,
    MIMES,
    IArtifactText,
} from '@starwhale/core/dataset'
import ImageViewer from '@starwhale/ui/Viewer/ImageViewer'
import AudioViewer from './AudioViewer'
import ImageGrayscaleViewer from './ImageGrayscaleViewer'
import TextViewer from './TextViewer'
import VideoViewer from './VideoViewer'

export type IDatasetViewerProps = {
    dataset?: any
    isZoom?: boolean
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

export default function DatasetViewer({ dataset, isZoom = false, hiddenLabels = new Set() }: IDatasetViewerProps) {
    const data = dataset

    const Viewer = React.useMemo(() => {
        if (!data) return <Placeholder />
        if (typeof data === 'number' || typeof data === 'string' || typeof data === 'boolean') {
            return <TextViewer data={data} isZoom={isZoom} />
        }

        const { _type, _mime_type: mimeType } = data

        switch (_type) {
            case ArtifactType.Image:
                if (mimeType === MIMES.GRAYSCALE) {
                    return <ImageGrayscaleViewer data={data as IArtifactImage} isZoom={isZoom} />
                }
                return (
                    <ImageViewer
                        data={data as IArtifactImage}
                        bboxes={dataset.bboxes ?? []}
                        cocos={dataset.cocos ?? []}
                        masks={dataset.masks ?? []}
                        isZoom={isZoom}
                        hiddenLabels={hiddenLabels}
                    />
                )
            case ArtifactType.Audio:
                return <AudioViewer data={data as IArtifactAudio} isZoom={isZoom} />
            case ArtifactType.Video:
                return <VideoViewer data={data as IArtifactVideo} isZoom={isZoom} />
            case ArtifactType.Text:
                return <TextViewer data={data as IArtifactText} isZoom={isZoom} />
            default:
                return <Placeholder />
        }
    }, [data, hiddenLabels, isZoom, dataset])

    if (!data) return <Placeholder />

    return Viewer
}
