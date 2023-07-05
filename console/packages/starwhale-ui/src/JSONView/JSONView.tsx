import React from 'react'
import { BusyPlaceholder } from '../BusyLoaderWrapper'
import { useCopyToClipboard } from 'react-use'

const RJV = React.lazy(() => import('react-json-view'))

function JSONView({
    data,
    collapsed = 1,
    collapseStringsAfterLength = 10,
}: {
    data: any
    collapsed?: number
    collapseStringsAfterLength?: number
}) {
    const [, copyToClipboard] = useCopyToClipboard()

    return (
        <React.Suspense fallback={<BusyPlaceholder />}>
            <RJV
                enableClipboard={(copy) => {
                    if (typeof copy.src === 'string') {
                        copyToClipboard(copy.src)
                    }
                    if (typeof copy.src === 'object') {
                        copyToClipboard(JSON.stringify(copy.src))
                    }
                }}
                collapsed={collapsed}
                src={data}
                name={false}
                collapseStringsAfterLength={collapseStringsAfterLength}
                displayDataTypes={false}
                quotesOnKeys={false}
            />
        </React.Suspense>
    )
}

export { JSONView }
export default JSONView
