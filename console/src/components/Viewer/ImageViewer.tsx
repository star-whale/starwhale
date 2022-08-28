import React from 'react'
import { TransformComponent, TransformWrapper } from 'react-zoom-pan-pinch'
import Button from '@/components/Button'
import normalLogoImg from '@/assets/logo_normal_en_white.svg'
import ZoomWrapper from './ZoomWrapper'
import { useEffect } from 'react'
import _ from 'lodash'
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
        let url = null
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
    }, [canvasRef, scale])

    return (
        <div className='flowContainer'>
            <Wrapper isTools={false}>
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
