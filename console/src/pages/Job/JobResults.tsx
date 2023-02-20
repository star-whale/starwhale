import LabelsIndicator from '@/components/Indicator/LabelsIndicator'
import React, { useMemo } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery } from 'react-query'
import { fetchJobResult } from '@/domain/job/services/job'
import { ILabels, INDICATORTYPE } from '@/components/Indicator/types'
import _ from 'lodash'
import { getHeatmapConfig, getRocAucConfig } from '@starwhale/ui/Plotly/utils'
import { LabelSmall } from 'baseui/typography'
import Card from '@/components/Card'
import useTranslation from '@/hooks/useTranslation'
import SummaryIndicator from '@/components/Indicator/SummaryIndicator'
import BusyPlaceholder from '@starwhale/ui/BusyLoaderWrapper/BusyPlaceholder'

const PlotlyVisualizer = React.lazy(() => import(/* webpackChunkName: "PlotlyVisualizer" */ '@starwhale/ui/Plotly'))

function JobResults() {
    const { jobId, projectId } = useParams<{ jobId: string; projectId: string }>()
    const jobResult = useQuery(`fetchJobResult:${projectId}:${jobId}`, () => fetchJobResult(projectId, jobId), {
        refetchOnWindowFocus: false,
    })

    const [t] = useTranslation()

    const indicators = useMemo(() => {
        return _.map(jobResult?.data, (v, k) => {
            let children = null
            let outTitle = ''

            switch (k) {
                default:
                    return <></>
                    break
                case INDICATORTYPE.KIND:
                    break
                case INDICATORTYPE.SUMMARY: {
                    const data = v
                    outTitle = t('Summary')
                    children = <SummaryIndicator data={data} />
                    break
                }
                case INDICATORTYPE.CONFUSION_MATRIX: {
                    const heatmapData = getHeatmapConfig(k, _.keys(v?.binarylabel), v?.binarylabel)
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
            </div>
        </div>
    )
}

export default React.memo(JobResults)
