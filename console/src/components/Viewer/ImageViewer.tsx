import React, { useEffect } from 'react'
import ZoomWrapper from './ZoomWrapper'
import { drawGrayscale } from './utils'

type IImageViewerProps = {
    isZoom?: boolean
    data: {
        src: string
    }
}
export default function ImageViewer({ isZoom = false, data }: IImageViewerProps) {
    const canvasRef = React.useRef<HTMLCanvasElement | null>(null)
    const Wrapper = isZoom ? ZoomWrapper : React.Fragment
    const scale = React.useMemo(() => {
        return isZoom ? 500 / 28 : 2
    }, [isZoom])

    useEffect(() => {
        if (canvasRef.current) {
            const canvas = canvasRef.current
            drawGrayscale(canvas, data.src, 28, 28, scale)
            // let blob: Blob = await new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg'))
            // url = URL.createObjectURL(blob)
            // setImage(url)
        }
        return () => {
            // console.log(url)
            // URL.revokeObjectURL(url)
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
                {/* <img src={image} width='auto' height='500px' /> */}
            </Wrapper>
        </div>
    )
}
