import React from 'react'
import { DatasetObject } from '@/domain/dataset/sdk'
import { createUseStyles } from 'react-jss'
import classnames from 'classnames'
import Plyr from 'plyr-react'
import 'plyr-react/plyr.css'

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

type IImageViewerProps = {
    isZoom?: boolean
    data: DatasetObject
}
const options = {
    debug: { enable: false },
    previewThumbnails: { enable: true },
}
export default function VideoViewer({ isZoom = false, data }: IImageViewerProps) {
    const playerRef = React.useRef<any>(null)
    const styles = useStyles()
    const { src, mimeType } = data

    const source = React.useMemo(() => {
        return {
            type: 'video',
            title: '',
            previewThumbnails: { enable: true },
            sources: [
                {
                    src,
                    type: mimeType,
                    size: 640,
                },
            ],
        }
    }, [src, mimeType])

    console.log(source)

    if (!isZoom) {
        return (
            <div
                className='dataset-viewer audio fullsize'
                style={{
                    height: '100%',
                    display: 'flex',
                    justifyContent: 'center',
                    alignItems: 'center',
                    position: 'relative',
                }}
            >
                {/* eslint-disable jsx-a11y/media-has-caption */}
                <Plyr
                    // @ts-ignore
                    ref={playerRef}
                    // @ts-ignore
                    source={source}
                    // @ts-ignore
                    options={options}
                    style={{
                        height: '100px',
                    }}
                />
            </div>
        )
    }

    return (
        <div className={classnames('dataset-viewer audio fullsize', styles.wrapper)}>
            <div className={styles.card}>
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
                <p className={styles.name}>{data?.data?.display_name ?? ''}</p>
            </div>
        </div>
    )
}
