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
    const Wrapper = isZoom ? ZoomWrapper : React.Fragment
    const scale = React.useMemo(() => {
        return isZoom ? 500 / 28 : 2
    }, [isZoom])

    useEffect(() => {
        if (canvasRef.current) {
            const canvas = canvasRef.current
            drawGrayscale(canvas, data.src, 28, 28, scale)
        }
    }, [canvasRef, scale, data])

    return (
        <div className='flowContainer'>
            <Wrapper
                // @ts-ignore
                isTools={isZoom ? false : undefined}
            >
                <canvas
                    ref={canvasRef}
                    style={{
                        zIndex: 1,
                        objectFit: 'contain',
                    }}
                />
            </Wrapper>
        </div>
    )
}
