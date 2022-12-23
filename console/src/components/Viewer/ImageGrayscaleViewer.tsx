import React, { useEffect } from 'react'
import ZoomWrapper from './ZoomWrapper'
import { drawGrayscale } from './utils'
import { IArtifactImage } from '../../domain/dataset/sdk'
import { useIsInViewport } from '@starwhale/core'

type IImageViewerProps = {
    isZoom?: boolean
    data: IArtifactImage
}
export default function ImageGrayscaleViewer({ isZoom = false, data }: IImageViewerProps) {
    const canvasRef = React.useRef<HTMLCanvasElement | null>(null)

    const isInView = useIsInViewport(canvasRef as HTMLElement)

    useEffect(() => {
        if (canvasRef.current && data.src && isInView) {
            const scale = isZoom ? 500 / 28 : 2
            const canvas = canvasRef.current
            drawGrayscale(canvas, data.src, 28, 28, scale)
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
