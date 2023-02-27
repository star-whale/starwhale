/* eslint-disable */
import React, { useEffect } from 'react'
import { TransformComponent, TransformWrapper } from 'react-zoom-pan-pinch'
import Button from '@starwhale/ui/Button'
import normalLogoImg from '@/assets/logo_normal_en_white.svg'
import ZoomWrapper from './ZoomWrapper'
import _, { reject } from 'lodash'
import Color from 'color'
import Plyr, { usePlyr } from 'plyr-react'
import 'plyr-react/plyr.css'
// import videoUrl from './video.mp4'
// import videoUrl3 from './video3.mp4'
// import videoUrl from './trailer.mp4'
import { Page } from '@/components/Pages/Page'
import test from './test.jpg'
import Card from '../../../../src/components/Card'
import 'rvfc-polyfill'

// import Select from '../Select'

// const VIDEO = 'https://upload.wikimedia.org/wikipedia/commons/a/a4/BBH_gravitational_lensing_of_gw150914.webm'
// const VIDEO = videoUrl
const VIDEO = 'https://cdn.plyr.io/static/demo/View_From_A_Blue_Moon_Trailer-576p.mp4'
const VIDEO_PLAY_BACK_RATE = 10.0
const DISPLAY_FRAMES_RATE = 10
const VIDEO_IMAGE_WIDTH = 300
const OPTIONS = [VIDEO]

async function initVideoElement(src: string) {
    const video = document.createElement('video')
    video.crossOrigin = 'anonymous'
    video.src = src
    video.width = 640
    video.defaultPlaybackRate = VIDEO_PLAY_BACK_RATE
    return video
}

type IFrame = { mediatime: number; src: string; meta: any }
function getVideoFrames(canvas: HTMLCanvasElement, video: HTMLVideoElement, src: string) {
    const ctx = canvas.getContext('2d')
    return new Promise((resolve, reject) => {
        if (!ctx) {
            return reject(new Error('Canvas not supported'))
        }
        let step = 1
        const frames: IFrame[] = []
        const drawingLoop = async (now: number, meta: any) => {
            // capture frame by FRAME RATE will accelerate extract process
            if (video.paused) {
                return
            }
            if (video.readyState >= 2) {
                step = Math.floor(video.duration / DISPLAY_FRAMES_RATE)
            } else {
                if (!video.ended) {
                    // @ts-ignore
                    video.requestVideoFrameCallback(drawingLoop)
                }
                return
            }

            const { width, height } = meta
            ctx.drawImage(video, 0, 0, VIDEO_IMAGE_WIDTH, (height / width) * VIDEO_IMAGE_WIDTH)
            // const bitmap = await createImageBitmap(video, 0, 0, video.width, video.height,)
            frames.push({ meta, mediatime: meta.mediaTime, src: canvas.toDataURL() })
            video.onended = (evt) => {
                console.log('ended', frames, resolve)
                return resolve(frames)
            }
            video.onerror = (evt) => {
                return reject(evt)
            }
            if (!video.ended) {
                // 0.1 to make sure video ended
                video.currentTime = Math.min(video.currentTime + step, video.duration) + 0.1
                // @ts-ignore
                video.requestVideoFrameCallback(drawingLoop)
            }
        }
        video.playbackRate = VIDEO_PLAY_BACK_RATE
        video.autoplay = true
        video.currentTime = 0
        video.muted = true
        video.src = src
        video.play()
        // @ts-ignore
        video.requestVideoFrameCallback(drawingLoop)
        // video.style.opacity = '0'
    })
}

async function getVideoFrameByTime(canvas: HTMLCanvasElement, video: HTMLVideoElement, time: number) {
    const ctx = canvas.getContext('2d')
    if (!ctx) {
        return
    }
    return new Promise((resolve, reject) => {
        const drawingLoop = async (now: number, meta: any) => {
            // const bitmap = await createImageBitmap(video)
            const { width, height } = meta
            ctx.drawImage(video, 0, 0, VIDEO_IMAGE_WIDTH, (height / width) * VIDEO_IMAGE_WIDTH)
            resolve({ meta, mediatime: meta.mediaTime, src: canvas.toDataURL() })
            video.pause()
            video.currentTime = 0
        }
        video.autoplay = true
        video.currentTime = time
        video.muted = true
        // @ts-ignore
        video.requestVideoFrameCallback(drawingLoop)
        // video.style.opacity = '0'
    })
}

// const useVideoFrames = (video: HTMLVideoElement, src: string) => {
//     const [frames, setFrames] = React.useState<IFrame[]>([])

//     useEffect(() => {
//         if (video) {
//             video.src = src
//             return
//         }
//         initVideoElement(src).then((video) => {
//             setVideo(video)
//         })
//     }, [video, src])

//     useEffect(() => {
//         if (video) {
//             getVideoFrames(video, src).then((rawframes: any) => {
//                 setFrames(rawframes)
//             })
//         }
//     }, [src, video])

//     return {
//         frames,
//     }
// }

// const drawFrames = (canvas: HTMLCanvasElement, frames: IFrame[]) => {
//     const ctx = canvas.getContext('2d')
//     if (!ctx || frames.length === 0) {
//         return []
//     }
//     const { width, height } = frames[0].meta
//     canvas.width = VIDEO_IMAGE_WIDTH
//     canvas.height = (height / width) * VIDEO_IMAGE_WIDTH
//     console.log(canvas.width, canvas.height, frames.length)

//     return frames
//         .map((frame, index) => {
//             ctx.drawImage(frame.bitmap, 0, 0, VIDEO_IMAGE_WIDTH, (height / width) * VIDEO_IMAGE_WIDTH)
//             return {
//                 mediatime: frame.mediatime,
//                 src: canvas.toDataURL(),
//             }
//         })
//         .filter((src) => !!src)
// }

const source = {
    type: 'video',
    title: 'Example title',
    previewThumbnails: { enable: true },
    sources: [
        // {
        //     src: 'bTqVqk7FSmY',
        //     provider: 'youtube',
        // },
        // {
        //     src: VIDEO,
        //     type: 'video/webm',
        //     size: 1080,
        // },
        {
            src: VIDEO,
            type: 'video/mp4',
            size: 640,
        },
    ],
}
const options = {
    debug: { enable: true },
    previewThumbnails: { enable: true },
}

export default function VideoPreviewViewer({ isZoom = true }) {
    const canvasRef = React.useRef<HTMLCanvasElement>(null)
    const playerRef = React.useRef<any>(null)
    const [frames, setFrames] = React.useState<IFrame[]>([])
    const [originX, setOriginX] = React.useState(0)
    const [previewImage, setPreviewImage] = React.useState<string>('')
    const [src, setSrc] = React.useState(OPTIONS[0])
    const [video, setVideo] = React.useState<HTMLVideoElement | null>(null)
    const canvas = canvasRef.current

    const imgs = React.useMemo(() => {
        if (!canvasRef.current || frames.length === 0) return []
        return frames
    }, [canvasRef, frames])

    useEffect(() => {
        if (!video || !canvas) return

        getVideoFrames(canvas, video, src).then((rawframes: any) => {
            console.log('new frames', rawframes)
            setFrames([...rawframes])
        })
    }, [video, src, setFrames, canvasRef])

    useEffect(() => {
        // TODO hacking way of get current hover frame time
        let player = playerRef.current?.plyr
        const id = setInterval(() => {
            if (!player?.elements?.progress) {
                return
            }
            clearInterval(id)
            player.elements.progress.addEventListener('mousemove', hoverEvent)
        }, 1000)

        const prevTime = 0
        const hoverEvent = _.throttle((event: any) => {
            const clientRect = player.elements.progress.getBoundingClientRect()
            const percentage = (100 / clientRect.width) * (event.pageX - clientRect.left)
            let seekTime = player.media.duration * (percentage / 100) ?? 0
            seekTime = Math.min(Math.max(0, seekTime), player.media.duration - 1)

            console.log(seekTime)
            if (!video || !canvas || Math.abs(seekTime - prevTime) < 1) {
                console.log('time too short to get frame')
                return
            }

            getVideoFrameByTime(canvas, video, seekTime).then((frame: any) => {
                setPreviewImage(frame.src)
            })
        }, 500)

        return () => {
            player?.elements?.progress?.removeEventListener('mousemove', hoverEvent)
            clearInterval(id)
            player = null
        }
    }, [playerRef, video, src])

    const handleGoto = React.useCallback(
        (time: number) => {
            const player = playerRef.current?.plyr
            // @ts-ignore
            if (player) player.currentTime = time
        },
        [playerRef]
    )

    const source = React.useMemo(() => {
        return {
            type: 'video',
            title: 'Example title',
            previewThumbnails: { enable: true },
            sources: [
                // {
                //     src: 'bTqVqk7FSmY',
                //     provider: 'youtube',
                // },
                // {
                //     src: VIDEO,
                //     type: 'video/webm',
                //     size: 1080,
                // },
                {
                    src,
                    type: 'video/mp4',
                    size: 640,
                },
            ],
        }
    }, [src])

    return (
        <div className='flowContainer '>
            <canvas ref={canvasRef} style={{ display: 'none' }} />
            {/* <Select
                // @ts-ignore
                value={{ id: src }}
                // @ts-ignore
                onChange={(params) => setSrc(params.option.id)}
                options={OPTIONS.map((op) => ({
                    // @ts-ignore
                    label: op,
                    id: op,
                }))}
                size='compact'
                clearable={false}
            /> */}

            <div
                style={{
                    display: 'grid',
                    gridTemplateColumns: '640px 640px',
                    gap: '20px',
                }}
            >
                <Card title='video player' style={{ margin: 0, padding: 0 }}>
                    <Plyr
                        // @ts-ignore
                        ref={playerRef}
                        // @ts-ignore
                        source={source}
                        // @ts-ignore
                        options={options}
                        style={{
                            width: '640px',
                        }}
                    />
                </Card>
                <Card title='peek image preview' style={{ margin: 0, padding: 0 }}>
                    <div>
                        <img src={previewImage} />
                    </div>
                </Card>

                <Card title='images slide preview' style={{ margin: 0, padding: 0 }}>
                    <div
                        style={{
                            // overflow: 'auto',
                            display: 'flex',
                            flexDirection: 'column',
                            alignItems: 'center',
                        }}
                    >
                        {imgs.length > 0 && (
                            <Page
                                originX={originX}
                                style={{
                                    width: 640,
                                    backgroundColor: 'rgba(255, 255, 255, .1)',
                                }}
                                onChangePage={(page) => {
                                    if (page < 0) return
                                    // @ts-ignore
                                    handleGoto(imgs[page].mediatime)
                                }}
                            >
                                {imgs?.map((item, i) => (
                                    <Button
                                        key={item?.mediatime + i}
                                        as='link'
                                        onClick={() => {
                                            handleGoto(item?.mediatime)
                                        }}
                                        overrides={{
                                            BaseButton: {
                                                style: {
                                                    position: 'relative',
                                                    userSelect: 'none',
                                                },
                                            },
                                        }}
                                    >
                                        <span
                                            style={{
                                                position: 'absolute',
                                                color: '#FFF',
                                                bottom: 0,
                                                left: 0,
                                                userSelect: 'none',
                                            }}
                                        >
                                            {item?.mediatime.toFixed(0)}s
                                        </span>
                                        <img
                                            key={i}
                                            src={item?.src as string}
                                            style={{
                                                userSelect: 'none',
                                            }}
                                        />
                                    </Button>
                                ))}
                            </Page>
                        )}
                    </div>
                    <h3>
                        <code>{`Page#${originX.toFixed(0)}`}</code>
                    </h3>
                    <input
                        type='range'
                        min={0}
                        max={Math.floor(imgs.length - 3)}
                        step={1}
                        value={originX}
                        style={{
                            width: '100%',
                        }}
                        onChange={(e) => {
                            setOriginX(parseFloat(e.target.value))
                            const page = Number(e.target.value)
                            handleGoto(imgs[page].mediatime)
                        }}
                    />
                </Card>

                <Card title='extracting video' style={{ margin: 0, padding: 0 }}>
                    <video
                        autoPlay
                        muted
                        ref={(ref) => setVideo(ref)}
                        width={VIDEO_IMAGE_WIDTH}
                        style={{ display: '1' }}
                        crossOrigin='anonymous'
                    />
                </Card>
            </div>
        </div>
    )
}
