import React, { useEffect } from 'react'
// @ts-ignore
import WaveformData from 'waveform-data'
import { DatasetObject } from '@/domain/dataset/sdk'
import { createUseStyles } from 'react-jss'
import classnames from 'classnames'
import { drawAudioWaveform } from './utils'

const useStyles = createUseStyles({
    wrapper: {
        height: '100%',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        padding: '20px ',
    },
    card: {
        'display': 'flex',
        'flexDirection': 'column',
        'width': '100%',
        'border': '1px solid #E2E7F0',
        'borderRadius': '4px',
        '& audio': {
            width: '100%',
        },
        'padding': '20px 20px 0 20px',
    },
    name: {
        height: '62px',
        lineHeight: '62px',
        borderTop: '1px solid #E2E7F0',
        marginTop: '10px',
    },
})

type IImageViewerProps = {
    isZoom?: boolean
    data: DatasetObject
}
export default function AudioViewer({ isZoom = false, data }: IImageViewerProps) {
    const canvasRef = React.useRef<HTMLCanvasElement | null>(null)
    const styles = useStyles()

    useEffect(() => {
        if (!canvasRef.current) return
        if (!data.src) return
        const canvas = canvasRef.current
        const audioContext = new AudioContext()
        fetch(data.src)
            .then((response) => response.arrayBuffer())
            .then((buffer) => {
                const options = {
                    audio_context: audioContext,
                    array_buffer: buffer,
                    scale: 128,
                }

                return new Promise((resolve, reject) => {
                    WaveformData.createFromAudio(options, (err: any, waveform: any) => {
                        if (err) {
                            reject(err)
                        } else {
                            resolve(waveform)
                        }
                    })
                })
            })
            .then((waveform: any) => {
                // console.log(`Waveform has ${waveform.channels} channels`)
                // console.log(`Waveform has length ${waveform.length} points`)
                drawAudioWaveform(canvas, waveform)
            })
    }, [canvasRef, data.src])

    if (!isZoom) {
        return (
            <div
                className='fullsize'
                style={{ height: '100%', display: 'flex', justifyContent: 'center', alignItems: 'center' }}
            >
                {/* eslint-disable jsx-a11y/media-has-caption */}
                <audio controls style={{ width: '100%' }}>
                    <source src={data.src} type={data.mimeType as string} />
                    Your browser does not support the audio element.
                </audio>
            </div>
        )
    }

    return (
        <div className={classnames('fullsize', styles.wrapper)}>
            <div className={styles.card}>
                <canvas
                    ref={canvasRef}
                    style={{
                        zIndex: 1,
                        objectFit: 'contain',
                        // position: 'absolute',
                    }}
                />
                {/* eslint-disable jsx-a11y/media-has-caption */}
                <audio controls>
                    <source src={data.src} type={data.mimeType as string} />
                    Your browser does not support the audio element.
                </audio>
                <p className={styles.name}>{data?.data?.display_name ?? ''}</p>
            </div>
        </div>
    )
}
