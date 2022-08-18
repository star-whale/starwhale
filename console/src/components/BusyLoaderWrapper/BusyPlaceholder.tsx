import React from 'react'
// eslint-disable-next-line baseui/deprecated-component-api
import { Spinner, SIZE } from 'baseui/spinner'
import IconFont from '../IconFont'

interface IBusyPlaceholderProps {
    type?: 'spinner' | 'loading' | 'notfound' | 'empty'
}

export default function BusyPlaceholder({ type }: IBusyPlaceholderProps) {
    let children = null

    switch (type) {
        default:
        case 'spinner':
            children = <Spinner $size={SIZE.large} $style={{ alignSelf: 'center' }} />
            break
        case 'notfound':
            children = (
                <>
                    <div style={{ alignSelf: 'center', fontSize: '50px' }}>
                        <IconFont type='searchEmpty' />
                    </div>
                </>
            )
            break
        case 'empty':
            children = (
                <>
                    <div style={{ alignSelf: 'center', fontSize: '50px' }}>
                        <IconFont type='empty' />
                    </div>
                </>
            )
            break
    }

    return (
        <div
            style={{
                display: 'flex',
                justifyContent: 'center',
                alignContent: 'center',
                flexDirection: 'column',
                width: '100%',
                height: '100%',
                minHeight: '500px',
            }}
        >
            {children}
        </div>
    )
}
