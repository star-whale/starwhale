import React, { useEffect } from 'react'
import { useIsInViewport } from '@starwhale/core'
import ZoomWrapper from './ZoomWrapper'
import { drawGrayscale } from './utils'
import { IArtifactImage } from '@starwhale/core/dataset'

type IImageViewerProps = {
    isZoom?: boolean
    data: IArtifactImage
}
export default function ImageGrayscaleViewer({ isZoom = false, data }: IImageViewerProps) {
    const canvasRef = React.useRef<HTMLCanvasElement | null>(null)

    const isInView = useIsInViewport(canvasRef as any)

    useEffect(() => {
        if (canvasRef.current && data._extendSrc && isInView) {
            const scale = isZoom ? 500 / 28 : 2
            const canvas = canvasRef.current
            drawGrayscale(canvas, data._extendSrc, 28, 28, scale)
        }
    }, [canvasRef, isZoom, data, isInView])

    if (!isZoom) {
        return (
            <div className='dataset-viewer image-grayscale fullsize'>
                <canvas
                    ref={canvasRef}
                    style={{
                        zIndex: 1,
                        objectFit: 'contain',
                        width: '100%',
                        height: '100%',
                    }}
                />
            </div>
        )
    }

    return (
        <div className='dataset-viewer image-grayscale fullsize'>
            <ZoomWrapper>
                <canvas
                    width={500}
                    height={500}
                    ref={canvasRef}
                    style={{
                        zIndex: 1,
                        objectFit: 'contain',
                    }}
                />
            </ZoomWrapper>
        </div>
    )
}
