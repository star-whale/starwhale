import React from 'react'
import IconFont from '@starwhale/ui/IconFont'
import {
    ArtifactType,
    IArtifactAudio,
    IArtifactImage,
    IArtifactVideo,
    MIMES,
    AnnotationType,
} from '@starwhale/core/dataset'
import ImageViewer from '@starwhale/ui/Viewer/ImageViewer'
import AudioViewer from './AudioViewer'
import ImageGrayscaleViewer from './ImageGrayscaleViewer'
import TextViewer from './TextViewer'
import VideoViewer from './VideoViewer'
import _ from 'lodash'
import { JSONView } from '../JSONView'

export type IDataViewerProps = {
    data?: any
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

export default function DataViewer({
    data: rawData,
    isZoom = false,
    hiddenLabels = new Set(),
    showKey,
}: IDataViewerProps) {
    const Viewer = React.useMemo(() => {
        const { summary } = rawData
        const data = summary?.get(showKey) ?? rawData.value
        if (!data) return null

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
            default:
                // @ts-ignore
                if (isZoom && _.isObject(data) && !data._extendSrc && !data._content) {
                    return (
                        <JSONView
                            data={data}
                            style={{ width: '100%' }}
                            collapsed={5}
                            collapseStringsAfterLength={300}
                        />
                    )
                }
                try {
                    return <TextViewer data={data} isZoom={isZoom} />
                } catch {
                    return <Placeholder />
                }
        }
    }, [rawData, hiddenLabels, isZoom, showKey])

    return Viewer
}
