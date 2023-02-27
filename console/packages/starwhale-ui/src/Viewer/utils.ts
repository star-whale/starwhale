import React, { useEffect } from 'react'
import Color from 'color'
import { IBBox } from '@starwhale/core/dataset'

export const RAW_COLORS = [
    '#df672a',
    '#c1433c',
    '#3d9e3e',
    '#ecbb33',
    '#926ccb',
    '#6bb59b',
    '#ad825c',
    '#c66b9e',
    '#a7b756',
]
export const COLORS = RAW_COLORS.map((c) => Color(c).rgb().array() as [number, number, number])

export const loadImage = (label: any, url: string) => {
    const src =
        url.startsWith('http') || url.startsWith('data:image') || url.startsWith('/api')
            ? url
            : `data:image/png;base64,${url}`
    return new Promise<{ label: any; img: ImageData }>((resolve, reject) => {
        const img = new Image()
        img.onload = () => {
            const canvas = document.createElement('canvas')
            canvas.width = img.width
            canvas.height = img.height
            const ctx = canvas.getContext('2d')
            if (ctx) {
                ctx.drawImage(img, 0, 0)
                resolve({
                    label,
                    img: ctx.getImageData(0, 0, img.width, img.height),
                })
            }
        }
        img.crossOrigin = 'anonymous'
        img.onerror = () => reject(new Error('Failed to load image'))
        img.src = src
    })
}

export const useImageData = (src: string) => {
    const [imageData, setImageData] = React.useState<ImageData | null>(null)

    useEffect(() => {
        const img = new Image()
        img.src = src
        img.crossOrigin = 'anonymous'
        img.onload = () => {
            const canvas = document.createElement('canvas')
            canvas.width = img.width
            canvas.height = img.height
            const ctx = canvas.getContext('2d')
            if (ctx) {
                ctx.drawImage(img, 0, 0)
                setImageData(ctx.getImageData(0, 0, img.width, img.height))
            }
        }
    }, [src])

    return {
        imageData,
    }
}

export const clearCanvas = (canvas: HTMLCanvasElement) => {
    const ctx = canvas.getContext('2d')
    ctx?.clearRect(0, 0, canvas.width, canvas.height)
}

export type IImageData = { label: any; img: ImageData }
export const drawSegment = (canvas: HTMLCanvasElement, imgDatas: IImageData[], rawDatas: any[]) => {
    const ctx = canvas.getContext('2d')
    const newImageData = new ImageData(canvas.width, canvas.height)
    for (let i = 0; i < newImageData.data.length; i += 4) {
        const rawIndex = imgDatas.findIndex((v) => v.img.data[i + 0] > 0)
        // eslint-disable-next-line no-continue
        if (rawIndex < 0) continue
        const [r, g, b, a = 255] = rawDatas[rawIndex]?.color
        newImageData.data[i] = r
        newImageData.data[i + 1] = g
        newImageData.data[i + 2] = b
        newImageData.data[i + 3] = rawDatas[rawIndex]?.isShow ? a : 0
    }
    ctx?.putImageData(newImageData, 0, 0)
    return newImageData
}

export const drawSegmentWithCOCOMask = (canvas: HTMLCanvasElement, imgDatas: IImageData[]) => {
    const ctx = canvas.getContext('2d')
    const newImageData = new ImageData(canvas.width, canvas.height)
    const colorMap = new Map()

    for (let i = 0; i < newImageData.data.length; i += 4) {
        const rawIndex = imgDatas.findIndex((v) => v.img.data[i + 0] > 0)
        if (rawIndex < 0) {
            newImageData.data[i] = 0
            newImageData.data[i + 1] = 0
            newImageData.data[i + 2] = 0
            newImageData.data[i + 3] = 0
        } else {
            const label = [
                imgDatas[rawIndex].img.data[i + 0],
                imgDatas[rawIndex].img.data[i + 1],
                imgDatas[rawIndex].img.data[i + 2],
            ].join(',')

            // [255,0,0]
            // [1,1,1]
            if (!colorMap.has(label)) colorMap.set(label, COLORS[colorMap.size % COLORS.length])

            const [r, g, b] = colorMap.get(label)
            newImageData.data[i] = r
            newImageData.data[i + 1] = g
            newImageData.data[i + 2] = b
            newImageData.data[i + 3] = 220
        }
    }

    ctx?.putImageData(newImageData, 0, 0)
    return newImageData
}

export const drawGrayscale = async (
    canvas: HTMLCanvasElement,
    src: string,
    width: number,
    height: number,
    scale = 1
) => {
    const blob = await fetch(src).then((r) => r.blob())
    const buffer = await blob.arrayBuffer()
    const data = new Uint8Array(buffer)

    const resizeWidth = width * scale
    const resizeHeight = height * scale
    // eslint-disable-next-line no-param-reassign
    canvas.width = resizeWidth
    // eslint-disable-next-line no-param-reassign
    canvas.height = resizeHeight
    const ctx = canvas.getContext('2d')
    const newImageData = new ImageData(width, height)
    if (ctx) {
        for (let i = 0; i < data.length; i += 1) {
            const index = i * 4
            const gray = data[i]
            newImageData.data[index] = gray
            newImageData.data[index + 1] = gray
            newImageData.data[index + 2] = gray
            newImageData.data[index + 3] = 255
        }
        // ctx.putImageData(newImageData, 0, 0, 0, 0, width * 2, height * 2)
        const ibm = await window.createImageBitmap(newImageData, 0, 0, newImageData.width, newImageData.height, {
            resizeWidth,
            resizeHeight,
        })
        // ctx.scale(resizeWidth / newImageData.width, resizeHeight / newImageData.height)
        ctx.drawImage(ibm, 0, 0)
        return newImageData
    }
    return newImageData
}

export async function resizeImageData(imageData: ImageData, width: number, height: number) {
    const resizeWidth = width ?? 0
    const resizeHeight = height ?? 0
    const ibm = await window.createImageBitmap(imageData, 0, 0, imageData.width, imageData.height, {
        resizeWidth,
        resizeHeight,
    })
    const canvas = document.createElement('canvas')
    canvas.width = resizeWidth
    canvas.height = resizeHeight
    const ctx = canvas.getContext('2d')
    if (ctx) {
        ctx.scale(resizeWidth / imageData.width, resizeHeight / imageData.height)
        ctx.drawImage(ibm, 0, 0)
        return ctx.getImageData(0, 0, resizeWidth, resizeHeight)
    }
    return new ImageData(0, 0)
}

export function drawAudioWaveform(canvas: HTMLCanvasElement, waveform: any) {
    const scaleY = (amplitude: number, height: number) => {
        const range = 256
        const offset = 128

        return height - ((amplitude + offset) * height) / range
    }
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    ctx.beginPath()
    const channel = waveform.channel(0)
    // Loop forwards, drawing the upper half of the waveform
    for (let x = 0; x < waveform.length; x++) {
        const val = channel.max_sample(x)
        ctx.lineTo(x + 0.5, scaleY(val, canvas.height) + 0.5)
    }

    // Loop backwards, drawing the lower half of the waveform
    for (let x = waveform.length - 1; x >= 0; x--) {
        const val = channel.min_sample(x)
        ctx.lineTo(x + 0.5, scaleY(val, canvas.height) + 0.5)
    }

    ctx.closePath()
    ctx.stroke()
    ctx.fill()
}

export function drawBox(canvas: HTMLCanvasElement, bbox: IBBox, id: number) {
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    const [x, y, w, h] = bbox
    const color = COLORS[id % COLORS.length]
    const lineWidth = 2
    ctx.lineWidth = lineWidth
    ctx.strokeStyle = `rgba(${[...color, 1].join(',')})`
    ctx.strokeRect(x, y, w, h)
    ctx.fillStyle = `rgba(${[...color, 0.1].join(',')})`
    ctx.fillRect(x, y, w, h)
}
