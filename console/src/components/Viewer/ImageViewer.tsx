import React, { useEffect } from 'react'
import ZoomWrapper from './ZoomWrapper'
import { clearCanvas, drawBox, drawGrayscale, drawSegmentWithCOCOMask, IImageData, loadImage } from './utils'
import { IAnnotationCOCOObject, IObjectImage } from '@/domain/dataset/sdk'
import { createUseStyles } from 'react-jss'

const useStyles = createUseStyles({
    canvas: {
        zIndex: 1,
        objectFit: 'contain',
        width: '100%',
        height: '100%',
    },
})

type IImageViewerProps = {
    isZoom?: boolean
    data: {
        src: string
    }
    masks: IObjectImage[]
    hiddenLabels: Set<number>
    cocos: IAnnotationCOCOObject[]
}
export default function ImageViewer({ isZoom = false, data, masks = [], cocos = [], hiddenLabels }: IImageViewerProps) {
    const canvasRef = React.useRef<HTMLCanvasElement | null>(null)

    if (!isZoom) {
        return (
            <div className='fullsize' style={{ height: '100%' }}>
                <img src={data.src} width='auto' height='100%' alt='dataset view' />
            </div>
        )
    }

    const $cocos = React.useMemo(() => {
        return cocos.filter((coco) => !hiddenLabels.has(coco.id))
    }, [cocos, hiddenLabels])

    console.log($cocos, hiddenLabels)

    return (
        <div className='fullsize' style={{ height: '100%' }}>
            <ZoomWrapper isTools={isZoom ? false : undefined}>
                <img src={data.src} width='auto' height='100%' alt='dataset view' />
                <SegmentOverlay masks={masks} />
                <COCOBBoxOverlay cocos={$cocos} />
            </ZoomWrapper>
        </div>
    )
}

export function SegmentOverlay({ masks = [] }: { masks: IObjectImage[] }) {
    const canvasRef = React.useRef<HTMLCanvasElement | null>(null)
    const [imgDatas, setImgDatas] = React.useState<IImageData[]>([])
    const styles = useStyles()
    useEffect(() => {
        if (masks.length === 0) return
        const getImages = async () => {
            return Promise.all(masks.map((m, i) => loadImage(i, m?._raw_base64_data)))
        }
        getImages().then((d) => setImgDatas(d))
    }, [masks])

    useEffect(() => {
        if (canvasRef.current && masks[0]) {
            const [height, width] = masks[0].shape
            const canvas = canvasRef.current
            canvas.width = width
            canvas.height = height
            drawSegmentWithCOCOMask(canvas, imgDatas)
        }
    }, [canvasRef, imgDatas, masks])

    if (masks.length == 0) {
        return <></>
    }

    return <canvas ref={canvasRef} className={styles.canvas} style={{ position: 'absolute', left: 0 }} />
}

export function COCOBBoxOverlay({ cocos = [] }: { cocos: IImageViewerProps['cocos'] }) {
    const canvasRef = React.useRef<HTMLCanvasElement>(null)
    const styles = useStyles()

    useEffect(() => {
        if (!canvasRef.current) return
        if (cocos.length === 0) return
        const canvas = canvasRef.current
        clearCanvas(canvas)
        const coco = cocos[0]
        const [height = 0, width = 0] = coco.segmentation?.size ?? []

        canvas.width = width
        canvas.height = height

        cocos.map((coco) => drawBox(canvas, coco.bbox, coco.id))

        // drawSegmentWithCOCOMask(canvas, imgDatas)
    }, [canvasRef, cocos])

    if (cocos.length == 0) {
        return <></>
    }

    return <canvas ref={canvasRef} className={styles.canvas} style={{ position: 'absolute', left: 0 }} />
}
