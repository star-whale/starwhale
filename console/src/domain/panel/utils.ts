// eslint-disable-next-line max-classes-per-file
import _ from 'lodash'
import { WidgetStateT, WidgetTreeNode } from '@starwhale/core'

interface IPanel {
    title: string
    height: number
    charts: IChart[]
}

interface ILayout {
    title: string
    panels: IPanel[]
}

interface ILayouts {
    layouts: ILayout[]
}

interface IChart {
    type: string
    table: string
    title?: string
    x?: string
    y?: string
    height?: number
}

class Chart {
    public table: string

    protected type: string

    protected interType: string

    constructor(protected data: IChart) {
        this.table = data.table
        this.type = data.type
        this.interType = ''
    }

    getInternalType() {
        return this.interType
    }

    toWidget(id: string): Record<string, any> {
        const ret = {
            id,
            type: this.type,
        }
        if (this.table) {
            _.merge(ret, {
                fieldConfig: {
                    data: {
                        chartTitle: this.data.title,
                        tableName: `{{prefix}}${this.table}`,
                        chartType: this.type,
                    },
                },
            })
        }
        return ret
    }
}

class Section extends Chart {
    protected interType = 'ui:section'

    toWidget(id: string): Record<string, any> {
        return _.merge(super.toWidget(id), {
            optionConfig: {
                title: this.data.title,
                layout: { height: this.data.height },
            },
        })
    }
}

class Table extends Chart {
    protected interType = 'ui:panel:table'
}

class Roc extends Chart {
    protected interType = 'ui:panel:rocauc'

    toWidget(id: string): Record<string, any> {
        const { x, y } = this.data
        return _.merge(super.toWidget(id), { fieldConfig: { data: { x, y } } })
    }
}

class ConfusionMatrix extends Chart {
    protected interType = 'ui:panel:confusion_matrix'
}

const mapping: Record<string, typeof Chart> = {
    table: Table,
    roc_auc: Roc,
    confusion_matrix: ConfusionMatrix,
}

function getChart(chart: IChart | IPanel): Chart | undefined {
    if ('charts' in chart) {
        return new Section({ type: 'section', table: '', ...chart })
    }
    if (chart.type in mapping) {
        return new mapping[chart.type](chart)
    }
    return undefined
}

export function tryParseSimplified(content: any): WidgetStateT | undefined {
    const data = content as ILayouts
    if (!data || !('layouts' in data)) {
        return undefined
    }
    const widgets: Record<string, any> = {}
    let root: WidgetTreeNode = { type: 'ui:dndList' }

    data.layouts.forEach((layout, layoutIndex) => {
        const layoutNode: WidgetTreeNode = {
            id: `layout-${layoutIndex}`,
            type: 'ui:dndList',
            children: [],
        }
        // support only one layout for now
        root = layoutNode
        layout.panels.forEach((panel, panelIndex) => {
            const p = getChart(panel)
            if (!p) {
                return
            }
            const panelId = `panel-${layoutIndex}-${panelIndex}`
            const t = p.getInternalType()
            const panelNode: WidgetTreeNode = { id: panelId, type: t, children: [] }
            layoutNode.children?.push(panelNode)
            widgets[panelId] = p.toWidget(panelId)
            panel.charts.forEach((c, chartIndex) => {
                const chart = getChart(c)
                if (!chart) {
                    return
                }
                const type = chart.getInternalType()
                const chartId = `chart-${layoutIndex}-${panelIndex}-${chartIndex}`
                const chartNode: WidgetTreeNode = { id: chartId, type }
                panelNode.children?.push(chartNode)
                widgets[chartId] = chart.toWidget(chartId)
            })
        })
    })

    return {
        key: 'widgets',
        tree: [root],
        widgets,
        defaults: {},
    }
}
