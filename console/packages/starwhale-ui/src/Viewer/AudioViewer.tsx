import React, { useEffect } from 'react'
// @ts-ignore
import WaveformData from 'waveform-data'
import { createUseStyles } from 'react-jss'
import classnames from 'classnames'
import { drawAudioWaveform } from './utils'
import { IArtifactAudio } from '@starwhale/core/dataset'

const useStyles = createUseStyles({
    wrapper: {
        height: '100%',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        padding: '20px ',
        overflow: 'auto',
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

type IAudioViewerProps = {
    isZoom?: boolean
    data: IArtifactAudio
}
export default function AudioViewer({ isZoom = false, data }: IAudioViewerProps) {
    const canvasRef = React.useRef<HTMLCanvasElement | null>(null)
    const styles = useStyles()

    useEffect(() => {
        if (!canvasRef.current) return
        if (!data._extendSrc) return
        const canvas = canvasRef.current
        const audioContext = new AudioContext()
        fetch(data._extendSrc)
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
    }, [canvasRef, data._extendSrc])

    if (!isZoom) {
        return (
            <div
                className='dataset-viewer audio '
                style={{
                    height: '100%',
                    display: 'flex',
                    justifyContent: 'center',
                    alignItems: 'center',
                    width: '100%',
                }}
            >
                {/* eslint-disable jsx-a11y/media-has-caption */}
                <audio controls style={{ width: '100%' }}>
                    <source src={data._extendSrc} type={data._mime_type as string} />
                    Your browser does not support the audio element.
                </audio>
            </div>
        )
    }

    return (
        <div className={classnames('dataset-viewer audio fullsize', styles.wrapper)}>
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
                    <source src={data._extendSrc} type={data._mime_type as string} />
                    Your browser does not support the audio element.
                </audio>
                <p className={styles.name}>{data.display_name ?? ''}</p>
            </div>
        </div>
    )
}
