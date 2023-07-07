import classNames from 'classnames'
import React, { useEffect } from 'react'
import { createUseStyles } from 'react-jss'
import _ from 'lodash'
import { Tooltip } from '../Tooltip'

const useStyles = createUseStyles({
    wrapper: {
        overflow: 'hidden',
    },
    text: {
        padding: '20px',
        textWrap: 'inherit',
    },
})

type ITextViewerProps = {
    isZoom?: boolean
    data: any
}

const utf8Decoder = new TextDecoder('utf-8')

export default function TextViewer({ isZoom = false, data }: ITextViewerProps) {
    const [text, setText] = React.useState('')
    const styles = useStyles()

    useEffect(() => {
        if (!_.isObject(data)) {
            setText(data)
            return
        }
        // @ts-ignore
        if (data._content) {
            // @ts-ignore
            setText(data._content)
            return
        }
        // @ts-ignore
        fetch(data._extendSrc)
            .then((response) => response.arrayBuffer())
            .then((buffer) => {
                setText(utf8Decoder.decode(buffer))
            })
    }, [data, setText])

    if (!isZoom) {
        return (
            <div className={classNames(styles.wrapper, 'dataset-viewer text line-clamp line-clamp-2')}>
                <Tooltip
                    content={() => (
                        <p
                            style={{
                                maxWidth: '500px',
                                whiteSpace: 'pre-wrap',
                            }}
                        >
                            {text ?? ''}
                        </p>
                    )}
                >
                    <p style={{ lineHeight: '1.5' }}>{text}</p>
                </Tooltip>
            </div>
        )
    }

    return (
        <div className='dataset-viewer text fullsize' style={{ height: '100%' }}>
            <pre className={styles.text}>{text}</pre>
        </div>
    )
}
