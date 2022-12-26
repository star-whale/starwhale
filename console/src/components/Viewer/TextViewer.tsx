import classNames from 'classnames'
import React, { useEffect } from 'react'
import { createUseStyles } from 'react-jss'
import { StatefulTooltip } from 'baseui/tooltip'
import { IArtifactText } from '@/domain/dataset/sdk'

const useStyles = createUseStyles({
    wrapper: {
        display: 'flex',
        placeItems: 'center',
        textOverflow: 'hidden',
    },
    text: {
        padding: '20px',
    },
})

type ITextViewerProps = {
    isZoom?: boolean
    data: IArtifactText
}

const utf8Decoder = new TextDecoder('utf-8')

export default function TextViewer({ isZoom = false, data }: ITextViewerProps) {
    const [text, setText] = React.useState('')
    const styles = useStyles()

    useEffect(() => {
        if (!data.src) return
        fetch(data.src)
            .then((response) => response.arrayBuffer())
            .then((buffer) => {
                setText(utf8Decoder.decode(buffer))
            })
    }, [data, setText])

    if (!isZoom) {
        return (
            <div className={classNames(styles.wrapper, 'dataset-viewer text ')}>
                <StatefulTooltip content={() => <p style={{ maxWidth: '300px' }}>{text ?? ''}</p>} placement='bottom'>
                    <p className='text-ellipsis' style={{ lineHeight: '1.5' }}>
                        {text}
                    </p>
                </StatefulTooltip>
            </div>
        )
    }

    return (
        <div className='dataset-viewer text fullsize' style={{ height: '100%' }}>
            <p className={styles.text}>{text}</p>
        </div>
    )
}
