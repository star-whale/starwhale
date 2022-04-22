import React, { useCallback, useRef } from 'react'
import { Spinner, SIZE } from 'baseui/spinner'
import { Skeleton } from 'baseui/skeleton'

export interface IBusyLoaderWrapperProps {
    isLoading: boolean
    className?: string
    children?: React.ReactElement | any
    loaderComponent?: React.ReactElement
    loaderType?: string
    loaderConfig?: object
    width?: string
    height?: string
    style?: React.CSSProperties
}

import './BusyLoaderWrapper.scss'
import BusyPlaceholder from './BusyPlaceholder'

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

    return (
        <>
            {isLoading ? (
                <div className={`BusyLoaderWrapper ${className}`} style={{ width, height }}>
                    {loaderComponent || loaderRender()}
                </div>
            ) : children ? (
                <div style={{ width, height, ...style }} ref={mountCard}>
                    {children}
                </div>
            ) : null}
        </>
    )
}

export default React.memo(BusyLoaderWrapper)
