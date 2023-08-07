import React, { useMemo } from 'react'
import Card from '@/components/Card'
import yaml from 'js-yaml'
import { useRuntimeVersion } from '@/domain/runtime/hooks/useRuntimeVersion'
import { JSONView } from '@starwhale/ui/JSONView'

export default function RuntimeVersionOverviewMeta() {
    const { runtimeVersion } = useRuntimeVersion()

    const jsonData = useMemo(() => {
        if (!runtimeVersion?.versionInfo.meta) return {}
        return yaml.load(runtimeVersion?.versionInfo.meta)
    }, [runtimeVersion?.versionInfo.meta])

    return (
        <Card
            style={{
                fontSize: '14px',
            }}
        >
            <div
                style={{
                    paddingLeft: '10px',
                }}
            >
                <JSONView data={jsonData} collapsed={2} collapseStringsAfterLength={100} />
            </div>
        </Card>
    )
}
