import React, { useEffect } from 'react'
import { createUseStyles } from 'react-jss'
import { clearCanvas, drawBox, drawSegmentWithCOCOMask, IImageData, loadImage } from './utils'
import ZoomWrapper from './ZoomWrapper'
import { IArtifactImage, ITypeBoundingBox, ITypeCOCOObjectAnnotation } from '@starwhale/core/dataset'

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
    data: IArtifactImage
    masks: IArtifactImage[]
    cocos: (ITypeCOCOObjectAnnotation & { _show: boolean })[]
    bboxes: (ITypeBoundingBox & { _show: boolean })[]
}
export default function ImageViewer({ isZoom = false, data, masks = [], cocos = [], bboxes = [] }: IImageViewerProps) {
    const img = React.useRef<HTMLImageElement>(null)
    const [rect, setRect] = React.useState<{ width: number; height: number }>({ width: 0, height: 0 })
    useEffect(() => {
        if (data._extendSrc && isZoom) {
            const tmp = new Image()
            tmp.src = data._extendSrc
            tmp.onload = () => {
                setRect({ width: tmp.width, height: tmp.height })
            }
        }
    }, [data._extendSrc, isZoom])

    if (!isZoom) {
        return (
            <div
                className='dataset-viewer image-overlay fullsize'
                style={{
                    height: '100%',
                    backgroundImage: `url(${data._extendSrc})`,
                    backgroundSize: 'cover',
                    backgroundRepeat: 'no-repeat',
                    backgroundPosition: 'center',
                }}
            >
                <img
                    src={data._extendSrc}
                    width='auto'
                    height='100%'
                    alt='dataset view'
                    style={{ visibility: 'hidden' }}
                />
            </div>
        )
    }

    const [height, width] = !rect.width ? data.shape : [rect.height, rect.width]

    return (
        <div className='dataset-viewer image-overlay fullsize' style={{ height: '100%' }}>
            <ZoomWrapper isTools={isZoom ? false : undefined}>
                <img ref={img} src={data._extendSrc} alt='dataset view' width={width} height={height} />
                {masks.map((mask, i) => (
                    // eslint-disable-next-line @typescript-eslint/no-use-before-define
                    <SegmentOverlay key={i} mask={mask} index={i} />
                ))}
                {/* eslint-disable-next-line @typescript-eslint/no-use-before-define */}
                <COCOBBoxOverlay cocos={cocos} />
                {/* eslint-disable-next-line @typescript-eslint/no-use-before-define */}
                <BBoxOverlay bboxes={bboxes} width={width} height={height} />
            </ZoomWrapper>
        </div>
    )
}

export function SegmentOverlay({ mask, index }: { mask: IArtifactImage; index: number }) {
    const canvasRef = React.useRef<HTMLCanvasElement | null>(null)
    const [imgDatas, setImgDatas] = React.useState<IImageData[]>([])
    const styles = useStyles()
    useEffect(() => {
        if (!mask) return
        const getImage = async () => {
            return loadImage(index, mask?._extendSrc as string)
        }
        getImage().then((d) => setImgDatas([d]))
    }, [mask, index])

    useEffect(() => {
        if (canvasRef.current && mask && imgDatas.length > 0) {
            const { height, width } = imgDatas[0].img
            if (!width || !height) return
            const canvas = canvasRef.current
            canvas.width = width
            canvas.height = height
            drawSegmentWithCOCOMask(canvas, imgDatas)
        }
    }, [canvasRef, imgDatas, mask])

    if (!mask) {
        return <></>
    }

    return <canvas ref={canvasRef} className={styles.canvas} style={{ position: 'absolute', left: 0 }} />
}

export function BBoxOverlay({
    bboxes = [],
    width: canvasWidth,
    height: canvasHeight,
}: {
    bboxes: (ITypeBoundingBox & { _show: boolean })[]
    width: number
    height: number
}) {
    const canvasRef = React.useRef<HTMLCanvasElement>(null)
    const styles = useStyles()

    useEffect(() => {
        if (!canvasRef.current) return
        if (bboxes.length === 0) return
        const canvas = canvasRef.current
        clearCanvas(canvas)
        canvas.width = canvasWidth
        canvas.height = canvasHeight

        bboxes.map(({ x, y, width, height, _show }, index) => _show && drawBox(canvas, [x, y, width, height], index))
    }, [canvasRef, bboxes, canvasWidth, canvasHeight])

    if (bboxes.length === 0) {
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
        const [height = 0, width = 0] = coco._segmentation_rle_size ?? []
        canvas.width = width
        canvas.height = height

        cocos.map((c, index) => drawBox(canvas, c.bbox, index))
    }, [canvasRef, cocos])

    if (cocos.length === 0) {
        return <></>
    }

    return <canvas ref={canvasRef} className={styles.canvas} style={{ position: 'absolute', left: 0 }} />
}
