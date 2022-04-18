import React, { useCallback, useRef } from 'react'

import { Spinner, SIZE } from 'baseui/spinner'
import { Skeleton } from 'baseui/skeleton'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary'

export interface IBusyLoaderWrapperProps {
    isLoading: boolean
    className?: string
    children?: React.ReactElement | any
    loaderComponent?: React.ReactElement
    loaderType?: string
    loaderConfig?: object
    width?: string
    height?: string
}

import './BusyLoaderWrapper.scss'

function BusyLoaderWrapper({
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
                return <Spinner size={SIZE.large} {...loaderConfig} />
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
                <ErrorBoundary>
                    <div className={`BusyLoaderWrapper ${className}`} style={{ width, height }}>
                        {loaderComponent || loaderRender()}
                    </div>
                </ErrorBoundary>
            ) : children ? (
                <ErrorBoundary>
                    <div style={{ width, height }} ref={mountCard}>
                        {children}
                    </div>
                </ErrorBoundary>
            ) : null}
        </>
    )
}

export default React.memo(BusyLoaderWrapper)
