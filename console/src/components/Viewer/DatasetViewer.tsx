import React from 'react'
import IconFont from '@/components/IconFont'
import {
    ArtifactType,
    DatasetObject,
    IArtifactAudio,
    IArtifactImage,
    IArtifactVideo,
    MIMES,
    IArtifactText,
} from '@/domain/dataset/sdk'
import ImageViewer from '@/components/Viewer/ImageViewer'
import AudioViewer from './AudioViewer'
import ImageGrayscaleViewer from './ImageGrayscaleViewer'
import TextViewer from './TextViewer'
import VideoViewer from './VideoViewer'

export type IDatasetViewerProps = {
    dataset?: DatasetObject
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
    const { data } = dataset || {}

    const Viewer = React.useMemo(() => {
        if (!dataset || !data?.src) return <Placeholder />

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
