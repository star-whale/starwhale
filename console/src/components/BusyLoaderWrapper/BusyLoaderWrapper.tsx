import React from 'react'

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
                return <Skeleton {...loaderConfig} />
            }
            default: {
                return <Spinner size={SIZE.large} {...loaderConfig} />
            }
        }
    }
    return (
        <>
            {isLoading ? (
                <ErrorBoundary>
                    <div className={`BusyLoaderWrapper ${className}`} style={{ width, height }}>
                        {loaderComponent || loaderRender()}
                    </div>
                </ErrorBoundary>
            ) : children ? (
                children
            ) : null}
        </>
    )
}

export default React.memo(BusyLoaderWrapper)
