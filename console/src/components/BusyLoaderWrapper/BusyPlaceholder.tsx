import React from 'react'
import { Spinner, SIZE } from 'baseui/spinner'

export default function BusyPlaceholder() {
    return (
        <div
            style={{
                display: 'flex',
                justifyContent: 'center',
                alignContent: 'center',
                flexDirection: 'column',
                width: '100%',
                height: '100%',
            }}
        >
            <Spinner $size={SIZE.large} $style={{ alignSelf: 'center' }} />
        </div>
    )
}
