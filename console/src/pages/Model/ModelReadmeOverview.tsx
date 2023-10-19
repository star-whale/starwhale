import { useCurrentModelFile } from '@/domain/model/hooks/useCurrentModelFile'
import { BusyPlaceholder } from '@starwhale/ui'
import React, { Suspense } from 'react'

const Markdown = React.lazy(() => import('@starwhale/ui/Markdown/Markdown'))

export default function ModelReadmeOverview() {
    const { content } = useCurrentModelFile('src/README.md')

    return (
        <div
            className='flex-column'
            style={{
                paddingTop: '20px',
                minHeight: '500px',
            }}
        >
            <Suspense fallback={<BusyPlaceholder />}>
                <Markdown>{content}</Markdown>
            </Suspense>
            {!content && <BusyPlaceholder type='empty' />}
        </div>
    )
}
