import React, { useEffect } from 'react'
import { getHeatmapConfig, getRocAucConfig } from '@/components/Indicator/utils'
import Card from '@/components/Card'
import useTranslation from '@/hooks/useTranslation'
import BusyPlaceholder from '@/components/BusyLoaderWrapper/BusyPlaceholder'
import { showTableName, tablesOfEvaluation } from '@/domain/datastore/utils'
import { useJob } from '@/domain/job/hooks/useJob'
import { useListDatastoreTables, useQueryDatastore } from '@/domain/datastore/hooks/useFetchDatastore'
import { useProject } from '@/domain/project/hooks/useProject'
import { useParseConfusionMatrix, useParseRocAuc } from '@/domain/datastore/hooks/useParseDatastore'
import Table from '@/components/Table'

const PlotlyVisualizer = React.lazy(
    () => import(/* webpackChunkName: "PlotlyVisualizer" */ '../../components/Indicator/PlotlyVisualizer')
)

const PAGE_TABLE_SIZE = 100

function ConfusionMatrix({ fetch }: any) {
    const { labels, binarylabel } = useParseConfusionMatrix(fetch?.data)

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

function RocAuc({ fetch, name }: { fetch: any; name: string }) {
    const rocAucData = useParseRocAuc(fetch?.data)
    const [t] = useTranslation()
    const title = t('Roc Auc')
    // @ts-ignore
    const rocaucData = getRocAucConfig(title, [], rocAucData)

    return (
        <Card outTitle={showTableName(name)} style={{ padding: '20px', background: '#fff', borderRadius: '12px' }}>
            <React.Suspense fallback={<BusyPlaceholder />}>
                <PlotlyVisualizer data={rocaucData} />
            </React.Suspense>
        </Card>
    )
}

function EvaluationViewer({ table }: { table: string }) {
    const query = React.useMemo(
        () => ({
            tableName: table,
            start: 0,
            limit: PAGE_TABLE_SIZE,
            rawResult: true,
            ignoreNonExistingTable: true,
        }),
        [table]
    )

    const info = useQueryDatastore(query, true)

    const columnTypes = React.useMemo(() => {
        if (!info.data) return {}
        return info.data?.columnTypes ?? {}
    }, [info])
    const columns = React.useMemo(() => {
        return Object.keys(columnTypes).sort((a) => (a === 'id' ? -1 : 1)) ?? []
    }, [columnTypes])
    const data = React.useMemo(() => {
        if (!info.data) return []

        return (
            info.data?.records?.map((item) => {
                return columns.map((k) => item?.[k])
            }) ?? []
        )
    }, [info.data, columns])

    if (info.isFetching) {
        return <BusyPlaceholder />
    }

    if (info.isError) {
        return <BusyPlaceholder type='notfound' />
    }
    // TODO hard code
    if (table.includes('confusion_matrix')) return <ConfusionMatrix name={table} fetch={info} />
    if (table.includes('roc_auc') && !table.includes('summary')) return <RocAuc name={table} fetch={info} />

    return (
        <Card outTitle={showTableName(table)} style={{ padding: '20px', background: '#fff', borderRadius: '12px' }}>
            <React.Suspense fallback={<BusyPlaceholder />}>
                <Table
                    columns={columns}
                    data={data}
                    // paginationProps={{
                    //     start: modelsInfo.data?.pageNum,
                    //     count: modelsInfo.data?.pageSize,
                    //     total: modelsInfo.data?.total,
                    //     afterPageChange: () => {
                    //         info.refetch()
                    //     },
                    // }}
                />
            </React.Suspense>
        </Card>
    )
}

function EvaluationResults() {
    const { project } = useProject()
    const { job } = useJob()

    const queryAllTables = React.useMemo(() => {
        if (!project?.name || !job?.uuid) return ''
        return {
            prefix: tablesOfEvaluation(project?.name as string, job?.uuid),
        }
    }, [project, job])
    const allTables = useListDatastoreTables(queryAllTables)

    useEffect(() => {
        if (job?.uuid && project?.name) {
            allTables.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [project?.name, job?.uuid])

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
                {allTables.data?.tables
                    ?.sort((a, b) => (a > b ? 1 : -1))
                    .map((name) => {
                        // TODO hard code
                        if (name.includes('results')) return <></>

                        return <EvaluationViewer table={name} key={name} />
                    })}
            </div>
        </div>
    )
}
export default EvaluationResults
