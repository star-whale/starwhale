import React, { useMemo } from 'react'
import Card from '@/components/Card'
import yaml from 'js-yaml'
import { useDatasetVersion } from '@/domain/dataset/hooks/useDatasetVersion'
import JSONView from '@starwhale/ui/JSONView'

export default function DatasetVersionMeta() {
    const { datasetVersion: dataset } = useDatasetVersion()

    const jsonData = useMemo(() => {
        if (!dataset?.versionMeta) return {}
        return yaml.load(dataset?.versionMeta)
    }, [dataset?.versionMeta])

    return (
        <Card
            style={{
                fontSize: '14px',
                // backgroundColor: 'rgb(0, 43, 54)',
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
