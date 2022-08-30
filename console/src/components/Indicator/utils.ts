// @ts-nocheck

import _ from 'lodash'

interface IRocAuc {
    fpr: number[]
    tpr: number[]
    thresholds: number[]
    auc: number
}

const Ticks = {
    full: {
        autotick: false,
        ticks: 'outside',
        tickcolor: 'rgb(204,204,204)',
        tickwidth: 2,
        ticklen: 5,
        tickfont: {
            family: 'Arial',
            size: 12,
            color: 'rgb(82, 82, 82)',
        },
    },
    auto: {
        autotick: true,
        ticks: 'outside',
        tickcolor: 'rgb(204,204,204)',
        tickwidth: 2,
        ticklen: 5,
        tickfont: {
            family: 'Arial',
            size: 12,
            color: 'rgb(82, 82, 82)',
        },
    },
    init: {},
}

const Layout = {
    init: {
        autosize: true,
        // width: 1000,
        // height: 1000,
        annotations: [] as any[],
        xaxis: {
            ...Ticks.full,
        },
        yaxis: {
            ...Ticks.full,
        },
        font: {
            family: 'Consolas',
            size: 16,
        },
    },
    auto: {
        autosize: true,
        annotations: [] as any[],
        xaxis: {
            ...Ticks.auto,
        },
        yaxis: {
            ...Ticks.auto,
        },
        font: {
            family: 'Consolas',
            size: 16,
        },
    },
}

export function getRocAucConfig(title = '', labels: string[], data: Record<string, IRocAuc>) {
    const layout = {
        ...Layout.init,
        title,
        xaxis: {
            ...Layout.init.xaxis,
            title: 'False Positive Rate',
            autotick: true,
        },
        yaxis: {
            ...Layout.init.yaxis,
            title: 'True Positive Rate',
            autotick: true,
        },
    }

    const rocAucData = {
        data: [
            ..._.map(data, (roc_auc, label) => {
                return {
                    x: roc_auc.fpr,
                    y: roc_auc.tpr,
                    mode: 'lines+markers',
                    name: `label ${label}`,
                    type: 'scatter',
                }
            }),
            {
                x: [0, 1],
                y: [0, 1],
                mode: 'lines',
                name: 'baseline',
                line: {
                    dash: 'dashdot',
                    width: 4,
                },
            },
        ],
        layout: {
            ...layout,
        },
    }
    return rocAucData
}

// columnTypes: {label_6: "FLOAT64", label_5: "FLOAT64", label_4: "FLOAT64", label_3: "FLOAT64", label_9: "FLOAT64",…}
// records: [0: {label_6: "3f1a378ebbf957e5", label_5: "0", label_4: "0", label_3: "0", label_9: "0",…}]

export function getHeatmapConfig(title = '', labels: string[], heatmap: number[][]) {
    const nums = labels.length
    let layout = {
        ...Layout.init,
        title,
    }

    if (nums > 10) {
        layout = {
            ...Layout.auto,
            title,
            width: nums * 30,
            height: nums * 30,
        }
    }

    const xValues = labels
    const yValues = labels
    const zValues = heatmap

    const annotations = []
    for (let i = 0; i < yValues.length; i++) {
        for (let j = 0; j < xValues.length; j++) {
            const currentValue = zValues[i][j]
            let textColor = ''
            if (currentValue !== 0) {
                textColor = 'white'
            } else {
                textColor = 'black'
            }
            const result = {
                xref: 'x1',
                yref: 'y1',
                x: xValues[j],
                y: yValues[i],
                text: zValues[i][j].toFixed(4),
                font: {
                    size: 14,
                    color: textColor,
                },
                showarrow: false,
            }

            if (i === j) annotations.push(result)
        }
    }

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
