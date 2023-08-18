import _ from 'lodash'
import { RJSFSchema, UiSchema } from '@rjsf/utils'
import {
    chartTitleField,
    chartTypeField,
    dataTableColumnsField,
    tableNameField,
    UI_DATA,
    UI_DATA_KEY,
} from './schemas/fields'
import WidgetModel from '../widget/WidgetModel'
import { ColumnSchemaDesc } from '../datastore'
import { WidgetConfig, WidgetFieldConfig } from '../types'

const PersistProperty = {
    type: true,
    name: true,
    group: true,
    description: true,
}

const DefaultFields = ['chartType', 'tableName', 'chartTitle']

const PanelUISchema: UiSchema = {
    'tableName': {
        'ui:widget': 'SelectWidget',
    },
    'ui:order': ['chartType', '*', 'chartTitle'],
    'ui:submitButtonOptions': {
        norender: true,
    },
}
// @FIXME remove none schema property
class WidgetFormModel implements WidgetFieldConfig {
    schema: RJSFSchema = {}

    uiSchema: UiSchema<any, RJSFSchema, any> = {}

    data: any

    widget?: WidgetModel

    $fields: RJSFSchema['properties']

    constructor(widget?: any) {
        if (widget) this.setWidget(widget)
        return this
    }

    setWidget(widget: WidgetModel) {
        this.widget = widget
        this.resetField()
        return this
    }

    resetField() {
        Object.keys(this.$fields ?? {}).forEach((field) => {
            if (DefaultFields.includes(field)) return
            delete this.$fields?.[field]
        })
        return this
    }

    addField(property?: RJSFSchema) {
        if (!property) return this

        this.$fields = {
            ...this.$fields,
            ...property,
        }
        return this
    }

    addDataTableNamesField(tables?: any[]) {
        const field = tableNameField(tables, this.widget?.config?.fieldConfig?.schema)
        this.addField(field)
        return this
    }

    addDataTableColumnsField(columnTypes?: ColumnSchemaDesc[]) {
        const { schema, uiSchema = {} } = this.widget?.config?.fieldConfig ?? {}
        Object.keys(uiSchema).forEach((property) => {
            if (uiSchema[property]?.[UI_DATA_KEY] === UI_DATA.DataTableColumns) {
                this.addField(dataTableColumnsField(property, columnTypes, schema))
            }
        })
        return this
    }

    removeField(property: string) {
        delete this.$fields?.[property]
        return this
    }

    initPanelSchema({ panelGroup }) {
        this.addField(chartTypeField(panelGroup))
        this.addField(chartTitleField())
        this.uiSchema = PanelUISchema
        return this
    }

    get schemas() {
        return {
            schema: {
                type: 'object',
                properties: _.merge({}, this.$fields, this.widget?.config?.fieldConfig?.schema),
            },
            uiSchema: _.merge({}, this.uiSchema, this.widget?.config?.fieldConfig?.uiSchema),
        }
    }

    getPersistProperty(): WidgetConfig {
        const properties = {} as WidgetConfig
        Object.keys(PersistProperty).forEach((key) => {
            ;(properties as any)[key] = (this as any)[key]
        })
        return properties
    }

    // saveToStore(api: WidgetStoreState) {
    //     return this
    // }
}

export default WidgetFormModel
