import BusyLoaderWrapper from '@/components/BusyLoaderWrapper/BusyLoaderWrapper'
import Card from '@/components/Card'
import { MBCConfusionMetricsIndicator } from '@/components/Indicator/MBCConfusionMetricsIndicator'
import { Spinner } from 'baseui/spinner'
import React, { useEffect, useMemo } from 'react'
import Plot from 'react-plotly.js'
import { useParams } from 'react-router-dom'
import { useQuery } from 'react-query'
import { fetchJobResult } from '@/domain/job/services/job'
import { IIndicator, IMBCConfusionMetrics, IMCConfusionMetrics, INDICATOR_TYPE } from '@/components/Indicator/types.d'
import _ from 'lodash'

const PlotlyVisualizer = React.lazy(
    () => import(/* webpackChunkName: "AudiosVisualizer" */ '@/components/Indicator/PlotlyVisualizer')
)

export default function JobResult() {
    const { jobId, projectId } = useParams<{ jobId: string; projectId: string }>()
    const jobResult = useQuery('fetchJobResult', () => fetchJobResult(jobId, projectId))
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

    const layout = {
        title: 'MCConfusionMetrics',
        annotations: [] as any[],
        xaxis: {
            ticks: '',
        },
        yaxis: {
            ticks: '',
            ticksuffix: ' ',
            width: 500,
            height: 500,
        },
        font: {
            family: 'Inter',
            size: 16,
        },
    }
    const xValues = labels
    const yValues = labels
    const zValues = heatmap

    for (var i = 0; i < yValues.length; i++) {
        for (var j = 0; j < xValues.length; j++) {
            var currentValue = zValues[i][j]
            if (currentValue != 0) {
                var textColor = 'white'
            } else {
                var textColor = 'black'
            }
            const result = {
                xref: 'x1',
                yref: 'y1',
                x: xValues[j],
                y: yValues[i],
                text: zValues[i][j],
                font: {
                    family: 'Inter',
                    size: 14,
                    color: textColor,
                },
                showarrow: false,
            }
            layout.annotations.push(result)
        }
    }
    const heatmapData = {
        data: [
            {
                x: xValues,
                y: yValues,
                z: zValues,
                colorscale: [
                    [0, '#3D9970'],
                    [1, '#001f3f'],
                ],
                // showscale: false,
                type: 'heatmap',
                // colorscale: 'Blackbody',
                // autocolorscale: true,
            },
        ],
        layout: {
            ...layout,
        },
    }

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
                <div
                    style={{
                        placeSelf: 'stretch',
                        padding: '20px',
                        background: '#fff',
                        borderRadius: '12px',
                    }}
                >
                    <MBCConfusionMetricsIndicator items={dataMBCConfusionMetrics} />
                </div>

                <div style={{ padding: '20px', background: '#fff', borderRadius: '12px' }}>
                    <React.Suspense fallback={<Spinner />}>
                        <PlotlyVisualizer data={heatmapData} />
                    </React.Suspense>
                </div>
            </div>
        </>
    )
}
