// @ts-nocheck

import _ from 'lodash'
import struct from '@aksel/structjs'

export interface IRocAuc {
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

function IEEEToDouble(is) {
    return new Float64Array(Uint32Array.from(is).buffer)[0]
}
function hex2bin(hex) {
    return parseInt(hex, 16).toString(2).padStart(8, '0')
}

var unhexlify2 = function (str) {
    var result = ''
    for (var i = 0, l = str.length; i < l; i += 2) {
        result += String.fromCharCode(parseInt(str.substr(i, 2), 16))
    }
    return result
}

var unhexlify = function (str) {
    var result = ''
    var f = new Uint8Array(8)
    var j = 0
    var bin = ''
    let accii
    for (var i = 0, l = str.length; i < l; i += 2) {
        // console.log(
        //     str.substr(i, 2),
        //     parseInt(str.substr(i, 2), 16)
        //     // String.fromCharCode(parseInt(str.substr(i, 2), 16))
        // )

        accii += String.fromCharCode(parseInt(str.substr(i, 2), 16))
        result += String(parseInt(str.substr(i, 2), 16))
        bin += String(hex2bin(str.substr(i, 2)))
        f[j] = parseInt(str.substr(i, 2), 16)
        j++
    }
    // console.log(str, f, IEEEToDouble(f), f.BYTES_PER_ELEMENT)
    // console.log(f.toString())
    // console.log(Uint32Array.from(f))
    // console.log(new Float64Array(Uint32Array.from(f).buffer)[0])
    // console.log(new Float64Array(Uint32Array.from(f).buffer)[1])
    // console.log(new Float64Array(Uint32Array.from(f).buffer)[2])
    // console.log(new Float64Array(Uint32Array.from(f).buffer)[3])
    // console.log(new Float64Array(Uint32Array.from(f).buffer)[4])
    // const decoder = new TextDecoder('utf8')
    // console.log(decoder.decode(f.buffer))
    // console.log(f.toString(10))
    // console.log(bin)
    // console.log(parseInt(bin, 2))

    let s = struct('>d')
    // console.log(result, unhexlify2(accii))
    try {
        // console.log(f)
        // console.log(str, f.toString(), s.unpack(f.buffer))
    } catch (e) {
        // console.log(e)
    }

    return s.unpack(f.buffer)[0]
}

var decrypt = function (a) {
    return a.replace(/\s*[01]{8}\s*/g, (bin) => {
        let charCode = parseInt(bin, 2)
        return String.fromCharCode(charCode)
    })
}

export function getRocAucConfig(title = '', labels: string[], data: IRocAuc[]) {
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

    const fpr = []
    const tpr = []
    const sorted = data?.sort((a, b) => {
        return parseInt(a.id) - parseInt(b.id)
    })
    data?.forEach((item, i) => {
        if (i % 6 != 0) return
        fpr.push(Number(unhexlify(item.fpr).toFixed('4')))
        tpr.push(Number(unhexlify(item.tpr).toFixed('4')))
    })

    const rocAucData = {
        data: [
            {
                x: fpr,
                y: tpr,
                mode: 'lines+markers',
                name: `label ${0}`,
                type: 'scatter',
            },
            {
                x: [0.0, 1],
                y: [0.0, 1],
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
    console.log(rocAucData.data[0])
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
