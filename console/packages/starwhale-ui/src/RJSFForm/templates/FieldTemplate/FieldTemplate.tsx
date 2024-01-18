import {
    FieldTemplateProps,
    FormContextType,
    RJSFSchema,
    StrictRJSFSchema,
    getTemplate,
    getUiOptions,
} from '@rjsf/utils'

import Label from './Label'

/** The `FieldTemplate` component is the template used by `SchemaField` to render any field. It renders the field
 * content, (label, description, children, errors and help) inside of a `WrapIfAdditional` component.
 *
 * @param props - The `FieldTemplateProps` for this component
 */
export default function FieldTemplate<
    T = any,
    S extends StrictRJSFSchema = RJSFSchema,
    F extends FormContextType = any
>(props: FieldTemplateProps<T, S, F>) {
    const { id, label, children, errors, help, description, hidden, required, displayLabel, registry, uiSchema } = props
    const uiOptions = getUiOptions(uiSchema)
    const WrapIfAdditionalTemplate = getTemplate<'WrapIfAdditionalTemplate', T, S, F>(
        'WrapIfAdditionalTemplate',
        registry,
        uiOptions
    )

    if (hidden) {
        return <div className='hidden'>{children}</div>
    }

    return (
        <WrapIfAdditionalTemplate {...props}>
            <p className='flex-0 basis-[25%] w-[25%] overflow-hidden text-end items-start justify-stretch lh-[32px] self-start mr-2'>
                <Label label={label} required={required} id={id} />
            </p>
            <div className='flex flex-col gap-[2px] flex-shrink-0'>
                {children}
                <div className='flex color-[rgba(2,16,43,0.60)]'>
                    {displayLabel && description ? description : null}
                    {errors}
                    {help}
                </div>
            </div>
        </WrapIfAdditionalTemplate>
    )
}
