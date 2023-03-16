import React from 'react'
// eslint-disable-next-line baseui/deprecated-component-api
import { Spinner, SIZE } from 'baseui/spinner'
import IconFont from '../IconFont'
import { LabelMedium } from 'baseui/typography'

interface IBusyPlaceholderProps {
    type?: 'spinner' | 'loading' | 'notfound' | 'empty' | 'center'
    style?: React.CSSProperties
    children?: React.ReactNode
}

export default function BusyPlaceholder({ type, style, children: rawChildren }: IBusyPlaceholderProps) {
    let children = rawChildren

    switch (type) {
        default:
        case 'spinner':
            children = <Spinner $size={SIZE.large} $style={{ alignSelf: 'center' }} />
            break
        case 'notfound':
            children = (
                <>
                    <div
                        style={{
                            alignSelf: 'center',
                            fontSize: '50px',
                            display: 'flex',
                            flexDirection: 'column',
                            justifyContent: 'center',
                            alignItems: 'center',
                            textAlign: 'center',
                            gap: '10px',
                        }}
                    >
                        <IconFont type='searchEmpty' size={50} />
                        <LabelMedium $style={{ color: 'rgba(2,16,43,0.20)' }}>{rawChildren}</LabelMedium>
                    </div>
                </>
            )
            break
        case 'empty':
            children = (
                <>
                    <div
                        style={{
                            alignSelf: 'center',
                            fontSize: '50px',
                            display: 'flex',
                            flexDirection: 'column',
                            justifyContent: 'center',
                            alignItems: 'center',
                            textAlign: 'center',
                            gap: '10px',
                        }}
                    >
                        <IconFont type='empty' size={50} />
                        <LabelMedium $style={{ color: 'rgba(2,16,43,0.20)' }}>{rawChildren}</LabelMedium>
                    </div>
                </>
            )
            break
        case 'center':
            children = (
                <>
                    <div style={{ display: 'grid', placeItems: 'center', gap: '12px' }}>{rawChildren}</div>
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
                ...style,
            }}
        >
            {children}
        </div>
    )
}
