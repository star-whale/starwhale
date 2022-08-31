import LabelsIndicator from '@/components/Indicator/LabelsIndicator'
import React, { useMemo, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery } from 'react-query'
import { fetchJobResult } from '@/domain/job/services/job'
import { ILabels, INDICATORTYPE } from '@/components/Indicator/types.d'
import _ from 'lodash'
import { getHeatmapConfig, getRocAucConfig, IRocAuc } from '@/components/Indicator/utils'
import { LabelSmall } from 'baseui/typography'
import Card from '@/components/Card'
import useTranslation from '@/hooks/useTranslation'
import SummaryIndicator from '@/components/Indicator/SummaryIndicator'
import BusyPlaceholder from '@/components/BusyLoaderWrapper/BusyPlaceholder'
import { tableNameOfConfusionMatrix, tableNameOfRocAuc } from '@/domain/datastore/utils'
import { useJob } from '@/domain/job/hooks/useJob'
import { useQueryDatasetList, useScanDatastore } from '@/domain/datastore/hooks/useFetchDatastore'
import { useProject } from '@/domain/project/hooks/useProject'
import { useParseConfusionMatrix } from '@/domain/datastore/hooks/useParseDatastore'

const PlotlyVisualizer = React.lazy(
    () => import(/* webpackChunkName: "PlotlyVisualizer" */ '../../components/Indicator/PlotlyVisualizer')
)

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

function RocAuc({ labels, data }: { labels: any[]; data: Record<string, IRocAuc> }) {
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
    const { jobId, projectId } = useParams<{ jobId: string; projectId: string }>()
    const jobResult = useQuery(`fetchJobResult:${projectId}:${jobId}`, () => fetchJobResult(projectId, jobId), {
        refetchOnWindowFocus: false,
    })
    const { project } = useProject()
    const { job } = useJob()
    const resultTableName = React.useMemo(() => {
        if (!project?.name || !job?.uuid) return ''
        return tableNameOfConfusionMatrix(project?.name as string, job?.uuid)
    }, [project, job])

    const resultTable = useQueryDatasetList(resultTableName, { pageNum: 0, pageSize: 1000 })
    // console.log(project?.name, resultTableName, resultTable)
    const { labels, binarylabel } = useParseConfusionMatrix(resultTable.data)

    const rocAucTable = useScanDatastore({
        tables: [{ tableName: tableNameOfRocAuc(project?.name as string, job?.uuid ?? '') }],
        start: 0,
        limit: 1000,
    })

    useEffect(() => {
        if (job?.uuid && project?.name) {
            rocAucTable.refetch()
        }
    }, [project?.name, job?.uuid])

    const [t] = useTranslation()

    const indicators = useMemo(() => {
        return _.map(jobResult?.data, (v, k) => {
            let children = null
            let outTitle = ''

            switch (k) {
                case INDICATORTYPE.KIND:
                    break
                default:
                case INDICATORTYPE.SUMMARY: {
                    const data = v
                    outTitle = t('Summary')
                    children = <SummaryIndicator data={data} />
                    break
                }
                case INDICATORTYPE.CONFUSION_MATRIX: {
                    const heatmapData = getHeatmapConfig(k, labels, v?.binarylabel)
                    outTitle = t('Confusion Matrix')
                    children = (
                        <React.Suspense fallback={<BusyPlaceholder />}>
                            <PlotlyVisualizer data={heatmapData} />
                        </React.Suspense>
                    )
                    break
                }
                case INDICATORTYPE.ROC_AUC: {
                    const rocaucData = getRocAucConfig(k, _.keys(v), v)
                    outTitle = t('Roc Auc')
                    children = (
                        <React.Suspense fallback={<BusyPlaceholder />}>
                            <PlotlyVisualizer data={rocaucData} />
                        </React.Suspense>
                    )
                    break
                }
                case INDICATORTYPE.LABELS:
                    _.forIn(v as ILabels, (subV, subK) => {
                        const [tp, fp, tn, fn] = _.flatten(
                            jobResult?.data?.[INDICATORTYPE.CONFUSION_MATRIX]?.multilabel?.[Number(subK)]
                        )

                        /* eslint-disable no-param-reassign */
                        subV = Object.assign(subV, {
                            tp,
                            fp,
                            tn,
                            fn,
                        })
                    })
                    outTitle = t('Labels')
                    children = <LabelsIndicator isLoading={jobResult.isLoading} data={v} />
                    break
            }
            return (
                children && (
                    <Card
                        outTitle={outTitle}
                        key={k}
                        style={{ padding: '20px', background: '#fff', borderRadius: '12px' }}
                    >
                        {children}
                    </Card>
                )
            )
        })
    }, [jobResult.data, jobResult.isLoading, t])

    if (jobResult.isFetching) {
        return <BusyPlaceholder />
    }

    if (jobResult.isError) {
        return <BusyPlaceholder type='notfound' />
    }

    return (
        <div style={{ width: '100%', height: 'auto' }}>
            {jobResult.data?.kind && (
                <div
                    key='kind'
                    style={{
                        boxSizing: 'border-box',
                        display: 'flex',
                    }}
                >
                    <div
                        style={{
                            width: 0,
                            height: '28px',
                            border: '4px solid',
                            borderColor: '#2B65D9',
                            marginRight: '2px',
                        }}
                    />
                    <LabelSmall
                        $style={{
                            textOverflow: 'ellipsis',
                            overflow: 'hidden',
                            whiteSpace: 'nowrap',
                            background: '#2B65D9',
                            lineHeight: '28px',
                            color: '#FFF',
                            paddingLeft: '12px',
                        }}
                    >
                        Type: {jobResult.data?.kind ?? ''}
                    </LabelSmall>
                    <div
                        style={{
                            width: 0,
                            height: 0,
                            border: '14px solid',
                            borderColor: '#2B65D9 transparent #2B65D9 #2B65D9',
                        }}
                    />
                </div>
            )}
            <div
                style={{
                    width: '100%',
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(800px, 1fr))',
                    gridGap: '16px',
                }}
            >
                {indicators}
                <Heatmap labels={labels} binarylabel={binarylabel} />
                <RocAuc labels={labels} data={rocAucTable.data?.records} />
            </div>
        </div>
    )
}

export default React.memo(EvaluationResults)
