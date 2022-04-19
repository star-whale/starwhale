import { useMemo } from 'react'

export function usetHeatmapConfig(labels: string[], heatmap: number[][]) {
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
    // TODO: big datas
    const xValues = labels
    const yValues = labels
    const zValues = heatmap

    const annotations = useMemo(() => {
        const annotations = []
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
                annotations.push(result)
            }
        }

        return annotations
    }, [labels, heatmap])

    layout.annotations = [...annotations]

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
    return heatmapData
}
