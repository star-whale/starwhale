import BusyLoaderWrapper from '@/components/BusyLoaderWrapper/BusyLoaderWrapper'
import Card from '@/components/Card'
import MBCConfusionMetricsIndicator from '@/components/Indicator/MBCConfusionMetricsIndicator'
import { Spinner } from 'baseui/spinner'
import React, { useEffect, useMemo } from 'react'
import Plot from 'react-plotly.js'
import { useParams } from 'react-router-dom'
import { useQuery } from 'react-query'
import { fetchJobResult } from '@/domain/job/services/job'
import { IIndicator, IMBCConfusionMetrics, IMCConfusionMetrics, INDICATOR_TYPE } from '@/components/Indicator/types.d'
import _ from 'lodash'
import { usetHeatmapConfig } from '@/components/Indicator/utils'

const PlotlyVisualizer = React.lazy(
    () => import(/* webpackChunkName: "PlotlyVisualizer" */ '../../components/Indicator/PlotlyVisualizer')
)

function JobResult() {
    const { jobId, projectId } = useParams<{ jobId: string; projectId: string }>()
    const jobResult = useQuery('fetchJobResult', () => fetchJobResult(projectId, jobId))
    useEffect(() => {
        if (jobResult.isSuccess) {
            console.log(jobResult.data)
        }
    }, [jobResult])

    const dataMBCConfusionMetrics = useMemo((): IMBCConfusionMetrics => {
        const indicator = jobResult?.data?.indicators.find(
            (v: IIndicator) => v.name === INDICATOR_TYPE.MBCConfusionMetrics
        )
        return indicator?.value ?? {}
    }, [jobResult, jobResult.data, jobResult.isSuccess])

    const dataMCConfusionMetrics = useMemo((): IMCConfusionMetrics => {
        const indicator = jobResult?.data?.indicators.find(
            (v: IIndicator) => v.name === INDICATOR_TYPE.MCConfusionMetrics
        )
        return indicator?.value ?? {}
    }, [jobResult, jobResult.data, jobResult.isSuccess])

    const labels = useMemo(() => {
        return _.keys(dataMBCConfusionMetrics) ?? []
    }, [dataMBCConfusionMetrics])

    const [heatmap, maxValue] = useMemo(() => {
        let metrics: number[][] = Array(labels.length)
            .fill(0)
            .map(() => Array(labels.length).fill(0))

        let maxValue = 0

        _.map(dataMCConfusionMetrics, (v, k) => {
            metrics[_.toNumber(v.label)][_.toNumber(v.prediction)] = v.value
            if (v.value > maxValue) {
                maxValue = v.value
            }
        })
        return [metrics, maxValue]
    }, [dataMCConfusionMetrics])

    const heatmapData = usetHeatmapConfig(labels, heatmap)

    return (
        <>
            <div
                style={{
                    width: '100%',
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(700px, 1fr))',
                    // gridAutoRows: '460px',
                    gridGap: '16px',
                    placeItems: 'stretch',
                }}
            >
                {dataMBCConfusionMetrics && (
                    <div
                        style={{
                            padding: '20px',
                            background: '#fff',
                            borderRadius: '12px',
                        }}
                    >
                        <MBCConfusionMetricsIndicator isLoading={jobResult.isLoading} data={dataMBCConfusionMetrics} />
                    </div>
                )}

                <div style={{ padding: '20px', background: '#fff', borderRadius: '12px' }}>
                    <React.Suspense fallback={<Spinner />}>
                        <PlotlyVisualizer data={heatmapData} />
                    </React.Suspense>
                </div>
            </div>
        </>
    )
}

export default React.memo(JobResult)
