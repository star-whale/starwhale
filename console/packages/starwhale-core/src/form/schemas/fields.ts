import { RJSFSchema, UiSchema } from '@rjsf/utils'
import { WidgetFactory } from '@starwhale/core/widget'

export const chartTypeField = (): RJSFSchema | undefined => {
    const panels = WidgetFactory.getPanels()
    if (panels.length === 0) return undefined

    return {
        chartType: {
            type: 'string',
            title: 'Chart Type',
            oneOf:
                WidgetFactory.getPanels().map((v) => ({
                    const: v.type,
                    title: v.name,
                })) ?? [],
        },
    }
}

export const tableNameField = (tables: any, schema?: RJSFSchema, uiSchema?: UiSchema): RJSFSchema | undefined => {
    if (tables.length === 0) return undefined
    const { type } = schema?.tableName ?? {}

    if (type === 'array') {
        return {
            type: 'array',
            uniqueItems: true,
            items: {
                type: 'object',
                oneOf:
                    tables.map((v: any) => ({
                        const: v.name,
                        title: v.short,
                    })) ?? [],
            },
        }
    }
    return {
        tableName: {
            type: 'string',
            title: 'Table Name',
            oneOf:
                tables.map((v: any) => ({
                    const: v.name,
                    title: v.short,
                })) ?? [],
        },
    }
}

export const chartTitleField = (): RJSFSchema | undefined => ({
    chartTitle: {
        title: 'Chart Title',
        type: 'string',
    },
})
