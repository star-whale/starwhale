import { RJSFSchema, UiSchema } from '@rjsf/utils'
import { EventBus } from '../events/types'
import { Matcher } from '../widget/utils/replacer'

export type WidgetMeta = Record<string, unknown>
export type WidgetType = string

// -----------store---------------
export type WidgetTreeNode = {
    id?: string
    type: string
    children?: WidgetTreeNode[]
    optionConfig?: Record<string, any>
    fieldConfig?: Record<string, any>
    [key: string]: any
}

export type WidgetStoreState = {
    isInit?: boolean
    editable?: boolean
    panelGroup?: WidgetGroupType[] | WidgetGroupType
    isEditable: () => boolean
    setRawConfigs: (configs: any) => void
    onConfigChange?: any
    onLayoutOrderChange?: any
    onLayoutChildrenChange?: any
    onWidgetChange?: any
    onWidgetDelete?: any
} & WidgetStateT &
    WideteExternalState

export type WidgetStateT = {
    key: string
    tree: WidgetTreeNode[]
    widgets: Record<string, any>
    defaults: Record<string, any>
}

export type WideteExternalState = {
    onStateChange?: any
}

// -----------the config of options/field---------------
export interface WidgetBaseConfig {
    type: WidgetType
    name: string
    group?: WidgetGroupType | WidgetGroupType[]
    description?: string
    meta?: WidgetMeta
}

export interface WidgetFieldConfig {
    uiSchema: UiSchema
    schema: RJSFSchema
    data: Record<string, any>
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

export interface WidgetProps<O extends object = any, F extends object = any>
    extends WidgetRendererProps<O, F>,
        WidgetTreeProps {}

export type WidgetComponent<O extends object = any, F extends object = any> = React.ComponentType<WidgetProps<O, F>>

// -----------widget renderer---------------
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
    onLayoutOrderChange?: (list: any[]) => void
    onLayoutChildrenChange?: (widgets: any, payload: Record<string, any>) => void
    onLayoutCurrentChange?: (widgets: any, payload: Record<string, any>) => void
    eventBus: EventBus
    // timeZone?: string
    width: number
    height: number
    children?: React.ReactNode
    page?: any
    onPageChange?: (page: any) => void
}

export type WidgetRendererType<O extends object = any, F extends object = any> = React.ComponentType<
    WidgetRendererProps<O, F>
>

export enum WidgetGroupType {
    ALL = 'ALL',
    PANEL = 'PANEL',
    LIST = 'LIST',
    REPORT = 'REPORT',
}
