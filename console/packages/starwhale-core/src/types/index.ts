import { WidgetStoreState, WidgetTreeNode } from '../store/store'
import { WidgetType } from '../widget/WidgetFactory'
import { EventBus } from '../events/types'
import { Matcher } from '../utils/replacer'

export type WidgetMeta = Record<string, unknown>

// -----------the config of options/field---------------

export interface WidgetBaseConfig {
    type: WidgetType
    name: string
    group?: WidgetGroupType
    description?: string
    meta?: WidgetMeta
}

export interface WidgetFieldConfig {
    uiSchema: any
    schema: any
    data: any
}

export interface WidgetDynamicConfig {
    matches: Matcher[]
    data: Record<string, any>
}

export type WidgetOptionConfig = any

export interface WidgetConfig<
    O extends WidgetOptionConfig = WidgetOptionConfig,
    F extends WidgetFieldConfig = WidgetFieldConfig
> extends WidgetBaseConfig {
    optionConfig?: Partial<O>
    fieldConfig?: Partial<F>
    dynamicConfig?: WidgetDynamicConfig
}

// -----------widget component---------------
export interface WidgetTreeProps {
    // @FIXME namepath
    id: string
    path?: any[]
    childWidgets?: WidgetTreeNode[]
}

// export interface WidgetActions {
//     onOrderChange?: () => any
// }

export interface WidgetProps<O extends object = any, F extends object = any>
    extends WidgetRendererProps<O, F>,
        WidgetTreeProps {}

export type WidgetComponent<O extends object = any, F extends object = any> = React.ComponentType<WidgetProps<O, F>>

// -----------widget renderer---------------

/**
 * Describes the properties that can be passed to the WidgetRenderer.
 *
 * @typeParam O - Config type for the widget being rendered.
 * @typeParam F - Field options type for the widget being rendered.
 *
 * @internal
 */
export interface WidgetRendererProps<O extends object = any, F extends object = any> {
    id: string
    type: string
    path?: any[]
    data?: any
    defaults: WidgetConfig
    optionConfig: Partial<O>
    fieldConfig: Partial<F>
    onOptionChange?: (options: Partial<O>) => void
    onFieldChange?: (options: Partial<F>) => void
    onLayoutOrderChange?: (oldIndex: number, newIndex: number) => void
    onLayoutChildrenChange?: (widgets: any, payload: Record<string, any>) => void
    onLayoutCurrentChange?: (widgets: any, payload: Record<string, any>) => void
    eventBus: EventBus
    // timeZone?: string
    width: number
    height: number
    children?: React.ReactNode
}

export type WidgetRendererType<O extends object = any, F extends object = any> = React.ComponentType<
    WidgetRendererProps<O, F>
>

// -----------WidgetGroup---------------

export enum WidgetGroupType {
    PANEL = 'PANEL',
    LIST = 'LIST',
}

type PanelTableProps = WidgetBaseConfig

// export type WidgetState = Record<string, unknown>
// export interface WidgetBuilder<
//   T extends WidgetProps,
//   S extends WidgetState
// > {
//   buildWidget(widgetProps: T): JSX.Element;
// }
