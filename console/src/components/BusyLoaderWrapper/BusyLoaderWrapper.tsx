import React, { useCallback } from 'react'
import { Skeleton } from 'baseui/skeleton'

import './BusyLoaderWrapper.scss'
import BusyPlaceholder from './BusyPlaceholder'

export interface IBusyLoaderWrapperProps {
    isLoading: boolean
    className?: string
    children?: React.ReactElement | any
    loaderComponent?: React.ReactElement
    loaderType?: string
    loaderConfig?: Record<string, unknown>
    width?: string
    height?: string
    style?: React.CSSProperties
}

function BusyLoaderWrapper({
    style = {},
    isLoading = false,
    className = '',
    children,
    loaderType = 'spinner',
    loaderConfig = {},
    width = '100%',
    height = 'auto',
    loaderComponent,
}: IBusyLoaderWrapperProps): React.FunctionComponentElement<React.ReactNode> | null {
    function loaderRender() {
        switch (loaderType) {
            case 'skeleton': {
                // eslint-disable-next-line
                return <Skeleton rows={5} height='100px' width='100%' animation {...loaderConfig} />
            }
            default: {
                return <BusyPlaceholder />
            }
        }
    }

    const mountCard = useCallback(
        (card) => {
            if (card) {
                // eslint-disable-next-line no-param-reassign
                card.style.transform = 'translate3d(0, 0, 0)'
            }
        },
        // eslint-disable-next-line react-hooks/exhaustive-deps
        []
    )

    if (isLoading) {
        return (
            <div className={`BusyLoaderWrapper ${className}`} style={{ width, height }}>
                {loaderComponent || loaderRender()}
            </div>
        )
    }

    return (
        <>
            {children ? (
                <div style={{ width, height, ...style }} ref={mountCard}>
                    {children}
                </div>
            ) : null}
        </>
    )
}

export default React.memo(BusyLoaderWrapper)
