import React from 'react'
import IconFont from '@starwhale/ui/IconFont'
import {
    ArtifactType,
    IArtifactAudio,
    IArtifactImage,
    IArtifactVideo,
    MIMES,
    IArtifactText,
    AnnotationType,
} from '@starwhale/core/dataset'
import ImageViewer from '@starwhale/ui/Viewer/ImageViewer'
import AudioViewer from './AudioViewer'
import ImageGrayscaleViewer from './ImageGrayscaleViewer'
import TextViewer from './TextViewer'
import VideoViewer from './VideoViewer'
import _ from 'lodash'

export type IDatasetViewerProps = {
    dataset?: any
    isZoom?: boolean
    hiddenLabels?: Set<number>
    showKey: string
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

export default function DatasetViewer({
    dataset,
    isZoom = false,
    hiddenLabels = new Set(),
    showKey,
}: IDatasetViewerProps) {
    // @ts-ignore
    const { summary } = dataset
    const data = summary?.get(showKey)

    const Viewer = React.useMemo(() => {
        if (!data) return <></>
        if (!_.isObject(data)) {
            return <TextViewer data={data} isZoom={isZoom} />
        }

        // @ts-ignore
        const { _type, _mime_type: mimeType } = data

        switch (_type) {
            case ArtifactType.Image: {
                if (mimeType === MIMES.GRAYSCALE) {
                    return <ImageGrayscaleViewer data={data as IArtifactImage} isZoom={isZoom} />
                }

                const bboxes = Array.from(summary)
                    .filter(([, value]) => value._extendType === AnnotationType.BOUNDINGBOX)
                    .map(([key, value]) => {
                        return {
                            ...value,
                            _show: !hiddenLabels.has(key),
                        }
                    })

                const cocos = Array.from(summary)
                    .filter(([, value]) => value._extendType === AnnotationType.COCO)
                    .map(([key, value]) => {
                        return {
                            ...value,
                            _show: !hiddenLabels.has(key),
                        }
                    })

                const masks =
                    Array.from(summary)
                        .filter(([key, value]) => !hiddenLabels.has(key) && value._extendType === AnnotationType.MASK)
                        .map(([, value]) => value) ?? []

                return (
                    <ImageViewer
                        data={data as IArtifactImage}
                        bboxes={bboxes}
                        cocos={cocos}
                        masks={masks}
                        isZoom={isZoom}
                    />
                )
            }
            case ArtifactType.Audio:
                return <AudioViewer data={data as IArtifactAudio} isZoom={isZoom} />
            case ArtifactType.Video:
                return <VideoViewer data={data as IArtifactVideo} isZoom={isZoom} />
            case ArtifactType.Text:
                return <TextViewer data={data as IArtifactText} isZoom={isZoom} />
            default:
                return <Placeholder />
        }
    }, [summary, data, hiddenLabels, isZoom])

    if (data === '') return <></>

    if (!data) return <Placeholder />

    return Viewer
}
