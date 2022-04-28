import React from 'react'
import { Spinner, SIZE } from 'baseui/spinner'

import { MdQueryStats } from 'react-icons/md'

interface IBusyPlaceholderProps {
    type?: 'spinner' | 'loading' | 'notfound'
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
                        <MdQueryStats />
                    </div>
                    <div style={{ alignSelf: 'center' }}>Not Found</div>
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
