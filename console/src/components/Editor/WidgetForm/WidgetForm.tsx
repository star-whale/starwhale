import Widgets from '@/components/RJSF/widgets'
import Form from '@rjsf/core'
import { RegistryWidgetsType, RJSFSchema, UiSchema } from '@rjsf/utils'
import validator from '@rjsf/validator-ajv8'
import React from 'react'
import { useJob } from '@/domain/job/hooks/useJob'
import { useProject } from '@/domain/project/hooks/useProject'
import useDatastoreTables from '../datastore/useDatastoreTables'
import WidgetFactory from '../Widget/WidgetFactory'

const uiSchema: UiSchema = {
    'tableName': {
        'ui:widget': 'SelectWidget',
    },
    // 'ui:order': ['bar', '*'],
    // "ui:options":  {
    //     expandable: false
    //   }
    'ui:submitButtonOptions': {
        norender: true,
    },
}

const formData = {
    title: 'First task',
    done: true,
}

function WidgetEditForm({ formData, onChange, onSubmit }, ref) {
    // 'starwhale', '90138e6fde2a480888531526b7b65dfe'
    const { project } = useProject()
    const { job } = useJob()
    const { tables = [] } = useDatastoreTables(project?.name, job?.uuid)

    const panels = WidgetFactory.getPanels()
    if (panels.length === 0) return <></>

    const tableName = {
        type: 'string',
        oneOf:
            tables.map((v) => ({
                const: v.name,
                title: v.short,
            })) ?? [],
    }
    const multiTableName = {
        type: 'array',
        uniqueItems: true,
        items: {
            type: 'object',
            oneOf:
                tables.map((v) => ({
                    const: v.name,
                    title: v.short,
                })) ?? [],
        },
    }

    const tableNameSchema = tableName // formData.chartType === 'ui:panel:table' ? tableName : multiTableName

    // console.log('panels', WidgetFactory.getPanels(), tables, tableNameSchema)

    const schema: RJSFSchema = {
        // title: 'My title',
        // description: 'My description',
        type: 'object',
        properties: {
            chartType: {
                type: 'string',
                oneOf:
                    WidgetFactory.getPanels().map((v) => ({
                        const: v.type,
                        title: v.name,
                    })) ?? [],
            },
            tableName: tables.length === 0 ? undefined : tableNameSchema,
            chartTitle: {
                type: 'string',
            },
        },
        //   "required": ["name"],
        //   "dependencies": {
        //     "credit_card": ["billing_address"]
        //   }
    }
    return (
        <Form
            schema={schema}
            widgets={Widgets}
            uiSchema={uiSchema}
            formData={formData}
            validator={validator}
            onSubmit={onSubmit}
            ref={(form) => (ref.current = form)}
            onChange={(e) => onChange?.(e.formData)}
        />
    )
}

export default React.forwardRef(WidgetEditForm)
