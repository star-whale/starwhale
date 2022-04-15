import BusyLoaderWrapper from '@/components/BusyLoaderWrapper/BusyLoaderWrapper'
import Card from '@/components/Card'
import { MBCConfusionMetricsIndicator } from '@/components/Indicator/MBCConfusionMetricsIndicator'
import { Spinner } from 'baseui/spinner'
import React from 'react'
import Plot from 'react-plotly.js'

type IDataType = 'MCConfusionMetrics' | 'CohenKappa' | 'MBCConfusionMetrics'

interface IMCConfusionMetrics {
    label: string
    prediction: string
    name: string
    value: number
}

interface IMBCConfusionMetrics {
    [s: string]: {
        tp: number
        tn: number
        fp: number
        fn: number
        accuracy: number
        precision: number
        recall: number
    }
}

interface IIndicator {
    name: IDataType
    value: IMBCConfusionMetrics
}

interface IDataProps {
    evaluationType: string
    indicators: Array<IIndicator>
    // IMCConfusionMetrics | IMBCConfusionMetrics | ICohenKappa
}

const data: IDataProps = {
    evaluationType: 'MCResultCollector',
    indicators: [
        // {
        //     name: 'MCConfusionMetrics',
        //     value: [
        //         {
        //             label: 'A',
        //             prediction: 'A',
        //             name: 'A-A',
        //             value: 3,
        //         },
        //         {
        //             label: 'A',
        //             prediction: 'B',
        //             name: 'A-B',
        //             value: 2,
        //         },
        //         {
        //             label: 'A',
        //             prediction: 'C',
        //             name: 'A-C',
        //             value: 0,
        //         },
        //     ],
        // },
        // {
        //     name: 'CohenKappa',
        //     value: 2.34,
        // },
        {
            name: 'MBCConfusionMetrics',
            value: {
                A: {
                    tp: 20,
                    tn: 30,
                    fp: 33,
                    fn: 22,
                    accuracy: 0.11,
                    precision: 0.22,
                    recall: 0.33,
                },
                B: {
                    tp: 20,
                    tn: 30,
                    fp: 33,
                    fn: 22,
                    accuracy: 0.11,
                    precision: 0.22,
                    recall: 0.33,
                },
                C: {
                    tp: 20,
                    tn: 30,
                    fp: 33,
                    fn: 22,
                    accuracy: 0.11,
                    precision: 0.22,
                    recall: 0.33,
                },
            },
        },
    ],
}

const PlotlyVisualizer = React.lazy(
    () => import(/* webpackChunkName: "AudiosVisualizer" */ '@/components/Indicator/PlotlyVisualizer')
)

export default function JobResult() {
    const plotlyData = {
        data: [
            {
                x: [0, 1, 2, 3, 4],
                y: [1, 5, 3, 7, 5],
                mode: 'lines+markers',
                type: 'scatter',
                line: { color: '#36425D' },
            },
        ],
        layout: {
            title: 'A Graph',
        },
    }
    const layout = {
        title: 'Annotated Heatmap',
        annotations: [],
        xaxis: {
            ticks: '',
            side: 'top',
        },
        yaxis: {
            ticks: '',
            ticksuffix: ' ',
            width: 700,
            height: 700,
            autosize: false,
        },
    }
    const xValues = ['A', 'B', 'C']
    const yValues = ['W', 'X', 'Y']
    const zValues = [
        [1, 20, 30],
        [20, 1, 60],
        [30, 60, 1],
    ]

    for (var i = 0; i < yValues.length; i++) {
        for (var j = 0; j < xValues.length; j++) {
            var currentValue = zValues[i][j]
            if (currentValue != 0.0) {
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
                    family: 'Arial',
                    size: 12,
                    color: textColor,
                },
                showarrow: false,
            }
            // layout.annotations.push(result)
        }
    }
    const heatmapData = {
        data: [
            {
                z: [
                    [0.0, 0.0, 0.75, 0.75, 0.0],
                    [0.0, 0.0, 0.75, 0.75, 0.0],
                    [0.75, 0.75, 0.75, 0.75, 0.75],
                    [0.0, 0.0, 0.0, 0.75, 0.0],
                ],
                colorscale: [
                    [0, '#3D9970'],
                    [1, '#001f3f'],
                ],
                showscale: false,
                type: 'heatmap',
            },
        ],
        layout: {
            ...layout,
        },
    }
    const indicator: IIndicator | undefined = data.indicators.find((v) => v.name === 'MBCConfusionMetrics')
    const MBCConfusionMetrics = indicator?.value || {}
    console.log(MBCConfusionMetrics)
    return (
        <>
            <Card>
                <MBCConfusionMetricsIndicator items={MBCConfusionMetrics} />
            </Card>
            <div
                style={{
                    width: '100%',
                    display: 'grid',
                    gridTemplateColumns: 'repeat(2, minmax(450px, 1fr))',
                    // gridAutoRows: '450px',
                    gridGap: '16px',
                }}
            >
                <Card>
                    <React.Suspense fallback={<Spinner />}>
                        <PlotlyVisualizer data={plotlyData} />
                    </React.Suspense>
                </Card>
                <Card>
                    <React.Suspense fallback={<Spinner />}>
                        <PlotlyVisualizer data={heatmapData} />
                    </React.Suspense>
                </Card>
            </div>
        </>
    )
}
