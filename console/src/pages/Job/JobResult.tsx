import BusyLoaderWrapper from '@/components/BusyLoaderWrapper/BusyLoaderWrapper'
import Card from '@/components/Card'
import { Spinner } from 'baseui/spinner'
import React from 'react'
import Plot from 'react-plotly.js'

const data = {
    evaluationType: 'MCResultCollector',
    indicators: [
        {
            name: 'MCConfusionMetrics',
            value: [
                {
                    label: 'A',
                    prediction: 'A',
                    name: 'A-A',
                    value: 3,
                },
                {
                    label: 'A',
                    prediction: 'B',
                    name: 'A-B',
                    value: 2,
                },
                {
                    label: 'A',
                    prediction: 'C',
                    name: 'A-C',
                    value: 0,
                },
            ],
        },
        {
            name: 'CohenKappa',
            value: 2.34,
        },
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
    return (
        <>
            <div
                style={{
                    width: '100%',
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(450px, 1fr))',
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
                        <PlotlyVisualizer data={plotlyData} />
                    </React.Suspense>
                </Card>
                <Card></Card>
            </div>
        </>
    )
}
