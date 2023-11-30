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
            children = (
                <>
                    <Spinner $size={SIZE.large} $style={{ alignSelf: 'center' }} />
                    {rawChildren && (
                        <LabelMedium $style={{ color: 'rgba(2,16,43,0.20)', textAlign: 'center', marginTop: '10px' }}>
                            {rawChildren}
                        </LabelMedium>
                    )}
                </>
            )
            break
        case 'notfound':
            children = (
                <>
                    <div
                        data-type='placeholder'
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
                        data-type='placeholder'
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
            data-type='placeholder'
            style={{
                display: 'flex',
                justifyContent: 'center',
                alignContent: 'center',
                flexDirection: 'column',
                width: '100%',
                height: '100%',
                flex: 1,
                ...style,
            }}
        >
            {children}
        </div>
    )
}
