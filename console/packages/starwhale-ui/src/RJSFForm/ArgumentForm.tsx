import Widgets from '@starwhale/ui/RJSFForm/widgets'
import Templates from '@starwhale/ui/RJSFForm/templates'
import Form from '@rjsf/core'
import validator from '@rjsf/validator-ajv8'
import React from 'react'
import ExtraWidgets from './Argument/widgets'
import ExtraTemplates from './Argument/templates'

// @ts-ignore
function ArgumentForm({ formData, onChange, onSubmit, form }: any, ref?: any) {
    const { schema, uiSchema } = form.schemas
    return (
        <Form
            showErrorList={false}
            schema={schema}
            widgets={{ ...Widgets, ...ExtraWidgets }}
            templates={{ ...Templates, ...ExtraTemplates }}
            uiSchema={uiSchema}
            formData={formData}
            validator={validator}
            onSubmit={onSubmit}
            // @ts-ignore
            ref={(f) => {
                if (ref)
                    // eslint-disable-next-line no-param-reassign
                    ref.current = f
            }}
            onChange={(e) => {
                onChange?.(e.formData)
            }}
        />
    )
}

export default React.forwardRef(ArgumentForm)
