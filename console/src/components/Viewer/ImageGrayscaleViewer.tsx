import React, { useEffect } from 'react'
import ZoomWrapper from './ZoomWrapper'
import { drawGrayscale } from './utils'

type IImageViewerProps = {
    isZoom?: boolean
    data: {
        src: string
    }
}
export default function ImageGrayscaleViewer({ isZoom = false, data }: IImageViewerProps) {
    const canvasRef = React.useRef<HTMLCanvasElement | null>(null)

    useEffect(() => {
        if (canvasRef.current) {
            const scale = isZoom ? 500 / 28 : 2
            const canvas = canvasRef.current
            drawGrayscale(canvas, data.src, 28, 28, scale)
        }
    }, [canvasRef, isZoom, data])

    if (!isZoom) {
        return (
            <div className='fullsize'>
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
        <div className='fullsize'>
            <ZoomWrapper>
                <canvas
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
