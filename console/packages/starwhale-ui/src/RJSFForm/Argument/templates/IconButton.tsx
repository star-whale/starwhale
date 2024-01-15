import { FormContextType, IconButtonProps, RJSFSchema, StrictRJSFSchema, TranslatableString } from '@rjsf/utils'
import { ExtendButton } from '@starwhale/ui/Button'

export default function IconButton<T = any, S extends StrictRJSFSchema = RJSFSchema, F extends FormContextType = any>(
    props: IconButtonProps<T, S, F>
) {
    const { icon, ...otherProps } = props
    return (
        // <button type='button' className={`btn btn-${iconType} ${className}`} {...otherProps}>
        //     <i className={`iconfont icon-${icon}`} />
        // </button>
        //
        // @ts-ignore
        <ExtendButton {...otherProps} icon={icon as any} kind='secondary' />
    )
}

export function AddButton<T = any, S extends StrictRJSFSchema = RJSFSchema, F extends FormContextType = any>(
    props: IconButtonProps<T, S, F>
) {
    const {
        registry: { translateString },
    } = props
    return (
        <IconButton
            title={translateString(TranslatableString.CopyButton)}
            className='array-item-copy'
            {...props}
            icon='add'
        />
    )
}

export function CopyButton<T = any, S extends StrictRJSFSchema = RJSFSchema, F extends FormContextType = any>(
    props: IconButtonProps<T, S, F>
) {
    const {
        registry: { translateString },
    } = props
    return (
        <IconButton
            title={translateString(TranslatableString.CopyButton)}
            className='array-item-copy'
            {...props}
            icon='copy'
        />
    )
}

export function MoveDownButton<T = any, S extends StrictRJSFSchema = RJSFSchema, F extends FormContextType = any>(
    props: IconButtonProps<T, S, F>
) {
    const {
        registry: { translateString },
    } = props
    return (
        <IconButton
            title={translateString(TranslatableString.MoveDownButton)}
            className='array-item-move-down'
            {...props}
            icon='decline'
        />
    )
}

export function MoveUpButton<T = any, S extends StrictRJSFSchema = RJSFSchema, F extends FormContextType = any>(
    props: IconButtonProps<T, S, F>
) {
    const {
        registry: { translateString },
    } = props
    return (
        <IconButton
            title={translateString(TranslatableString.MoveUpButton)}
            className='array-item-move-up'
            {...props}
            icon='rise'
        />
    )
}

export function RemoveButton<T = any, S extends StrictRJSFSchema = RJSFSchema, F extends FormContextType = any>(
    props: IconButtonProps<T, S, F>
) {
    const {
        registry: { translateString },
    } = props
    return (
        <IconButton
            title={translateString(TranslatableString.RemoveButton)}
            className='array-item-remove'
            {...props}
            iconType='danger'
            icon='close'
        />
    )
}
