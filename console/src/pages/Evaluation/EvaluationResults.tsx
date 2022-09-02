import React, { useEffect } from 'react'
import { getHeatmapConfig, getRocAucConfig } from '@/components/Indicator/utils'
import Card from '@/components/Card'
import useTranslation from '@/hooks/useTranslation'
import BusyPlaceholder from '@/components/BusyLoaderWrapper/BusyPlaceholder'
import { tableNameOfConfusionMatrix, tableNameOfRocAuc } from '@/domain/datastore/utils'
import { useJob } from '@/domain/job/hooks/useJob'
import { useQueryDatasetList, useScanDatastore } from '@/domain/datastore/hooks/useFetchDatastore'
import { useProject } from '@/domain/project/hooks/useProject'
import { useParseConfusionMatrix, useParseRocAuc } from '@/domain/datastore/hooks/useParseDatastore'

const PlotlyVisualizer = React.lazy(
    () => import(/* webpackChunkName: "PlotlyVisualizer" */ '../../components/Indicator/PlotlyVisualizer')
)

const p = { pageNum: 0, pageSize: 1000 }
function Heatmap({ labels, binarylabel }: any) {
    const [t] = useTranslation()
    const heatmapData = getHeatmapConfig(t('Confusion Matrix'), labels, binarylabel)
    return (
        <Card outTitle={t('Confusion Matrix')} style={{ padding: '20px', background: '#fff', borderRadius: '12px' }}>
            <React.Suspense fallback={<BusyPlaceholder />}>
                <PlotlyVisualizer data={heatmapData} />
            </React.Suspense>
        </Card>
    )
}

function RocAuc({ labels, data }: { labels: any[]; data: any }) {
    const [t] = useTranslation()
    const title = t('Roc Auc')
    const rocaucData = getRocAucConfig(title, labels, data)

    return (
        <Card outTitle={t('Roc Auc')} style={{ padding: '20px', background: '#fff', borderRadius: '12px' }}>
            <React.Suspense fallback={<BusyPlaceholder />}>
                <PlotlyVisualizer data={rocaucData} />
            </React.Suspense>
        </Card>
    )
}

function EvaluationResults() {
    const { project } = useProject()
    const { job } = useJob()
    const resultTableName = React.useMemo(() => {
        if (!project?.name || !job?.uuid) return ''
        return tableNameOfConfusionMatrix(project?.name as string, job?.uuid)
    }, [project, job])

    const resultTable = useQueryDatasetList(resultTableName, p, true)
    const { labels, binarylabel } = useParseConfusionMatrix(resultTable.data)
    const rocAucTable = useScanDatastore({
        tables: [{ tableName: tableNameOfRocAuc(project?.name as string, job?.uuid ?? '', '0') }],
        start: 0,
        limit: 1000,
        rawResult: true,
    })
    const rocAucData = useParseRocAuc(rocAucTable.data)

    useEffect(() => {
        if (job?.uuid && project?.name) {
            rocAucTable.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [project?.name, job?.uuid])

    if (resultTable.isFetching) {
        return <BusyPlaceholder />
    }

    if (resultTable.isError) {
        return <BusyPlaceholder type='notfound' />
    }

    return (
        <div style={{ width: '100%', height: 'auto' }}>
            <div
                style={{
                    width: '100%',
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(800px, 1fr))',
                    gridGap: '16px',
                }}
            >
                <Heatmap labels={labels} binarylabel={binarylabel} />
                {/* @ts-ignore */}
                <RocAuc labels={labels} data={rocAucData} />
            </div>
        </div>
    )
}

export default React.memo(EvaluationResults)
