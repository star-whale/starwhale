import { WidgetBaseConfig, WidgetConfig, WidgetFieldConfig, WidgetGroupType, WidgetMeta } from '../types/index'
import { WidgetType } from '../widget/WidgetFactory'
import WidgetFactory from '@starwhale/core/widget/WidgetFactory'
import { generateId } from '../utils/generators'
import { Matcher, PANEL_DYNAMIC_MATCHES, replacer, Replacer } from '../utils/replacer'
import { klona } from 'klona/full'
import _ from 'lodash'
import { StoreType } from '../context'
import { WidgetStoreState } from '../store/store'
import { RJSFSchema, UiSchema } from '@rjsf/utils'
import {
    chartTitleField,
    chartTypeField,
    dataTableColumnsField,
    tableNameField,
    UI_DATA,
    UI_DATA_KEY,
} from './schemas/fields'
import { Console } from 'console'
import WidgetModel from '../widget/WidgetModel'
import { ColumnSchemaDesc } from '../datastore'

const PersistProperty = {
    type: true,
    name: true,
    group: true,
    description: true,
}

const PanelUISchema: UiSchema = {
    'tableName': {
        'ui:widget': 'SelectWidget',
    },
    'ui:order': ['*', 'chartTitle'],
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
        return this
    }

    addField(property?: RJSFSchema) {
        if (!property) return

        this.$fields = {
            ...this.$fields,
            ...property,
        }
        return this
    }

    addDataTableNamesField(tables?: any[]) {
        if (!tables) return

        this.addField(tableNameField(tables, this.widget?.config?.fieldConfig?.schema))
    }

    addDataTableColumnsField(columnTypes?: ColumnSchemaDesc[]) {
        if (!columnTypes) return

        const { schema, uiSchema } = this.widget?.config?.fieldConfig ?? {}
        for (const property in uiSchema) {
            if (uiSchema[property]?.[UI_DATA_KEY] === UI_DATA.DataTableColumns) {
                this.addField(dataTableColumnsField(property, columnTypes, schema, uiSchema))
            }
        }
    }

    initPanelSchema() {
        this.addField(chartTypeField())
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
        for (const key in PersistProperty) {
            ;(properties as any)[key] = (this as any)[key]
        }
        return properties
    }

    saveToStore(api: WidgetStoreState) {}
}

export default WidgetFormModel
