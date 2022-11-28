import Widgets from '@starwhale/ui/form/widgets'
import Form from '@rjsf/core'
import { RegistryWidgetsType, RJSFSchema, UiSchema } from '@rjsf/utils'
import validator from '@rjsf/validator-ajv8'
import React from 'react'
import { useJob } from '@/domain/job/hooks/useJob'
import { useProject } from '@/domain/project/hooks/useProject'
import useDatastoreTables from '../datastore/hooks/useDatastoreTables'
import WidgetFactory from '../widget/WidgetFactory'

// @ts-ignore
function WidgetForm({ formData, onChange, onSubmit, form }: any, ref: any) {
    const { schema, uiSchema } = form.schemas
    return (
        <Form
            schema={schema}
            widgets={Widgets}
            uiSchema={uiSchema}
            formData={formData}
            validator={validator}
            onSubmit={onSubmit}
            // @ts-ignore
            ref={(form) => (ref.current = form)}
            onChange={(e) => onChange?.(e.formData)}
        />
    )
}

export default React.forwardRef(WidgetForm)
