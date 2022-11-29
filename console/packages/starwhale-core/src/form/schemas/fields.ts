import { RJSFSchema, UiSchema } from '@rjsf/utils'
import { ColumnSchemaDesc } from '@starwhale/core/datastore'
import { WidgetFactory } from '@starwhale/core/widget'

export const UI_DATA_KEY = 'ui:data'
export enum UI_DATA {
    DataTableColumns = 'DataTableColumns',
}

export const chartTypeField = (): RJSFSchema | undefined => {
    const panels = WidgetFactory.getPanels()
    if (!panels || panels.length === 0) return undefined

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
    if (!tables || tables.length === 0) return undefined
    const { type } = schema?.tableName ?? {}

    if (type === 'array') {
        return {
            tableName: {
                type: 'array',
                title: 'Table Name',
                uniqueItems: true,
                items: {
                    type: 'string',
                    oneOf:
                        tables.map((v: any) => ({
                            const: v.name,
                            title: v.short,
                        })) ?? [],
                },
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

export const dataTableColumnsField = (
    property: string,
    columnTypes: ColumnSchemaDesc[],
    schema?: RJSFSchema,
    uiSchema?: UiSchema
): RJSFSchema | undefined => {
    if (!columnTypes || columnTypes.length === 0) return undefined

    return {
        [property]: {
            type: 'string',
            title: schema?.title,
            oneOf:
                columnTypes.map((v) => ({
                    const: v.name,
                    title: v.name,
                })) ?? [],
        },
    }
}
