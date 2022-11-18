import { WidgetStoreState, WidgetTreeNode } from '../context/store'
import { WidgetConfig, WidgetType } from './WidgetFactory'
import { EventBus } from '../events/types'

export type WidgetMeta = Record<string, unknown>

// export const CONFIG = {
//     type: 'ui:panel:table',
//     group: 'panel',
//     name: 'table',
//     fieldConfig: {
//         uiSchema: {
//             'ui:order': ['*', 'chartTitle'],
//         },
//         schema: {
//             type: 'object',
//             properties: {
//                 tableName: {
//                     'ui:widget': 'DatastoreTableSelect',
//                 },
//             },
//         },
//         dataDefaults: {
//             chartType: 'ui:panel:table',
//         },
//         // dataOverrides: {
//         //     tableName: '',
//         //     chartTitle: 'summary',
//         // },
//     },
// }

// -----------the config of options/field---------------

export interface WidgetBaseConfig {
    type: WidgetType
    name: string
    group?: WidgetGroupType
    meta?: WidgetMeta
}

export interface WidgetFieldConfig {
    uiSchema: any
    schema: any
    data: any
}

export type WidgetOptionConfig = any

export interface WidgetConfig<
    O extends WidgetOptionConfig = WidgetOptionConfig,
    F extends WidgetFieldConfig = WidgetFieldConfig
> extends WidgetBaseConfig {
    optionConfig?: Partial<O>
    fieldConfig?: Partial<F>
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

export interface WidgetProps<C extends object = any, F extends object = any>
    extends WidgetRendererProps,
        WidgetTreeProps {}

export type WidgetComponent<C extends object = any, F extends object = any> = React.ComponentType<WidgetProps<C, F>>

// -----------widget renderer---------------

/**
 * Describes the properties that can be passed to the WidgetRenderer.
 *
 * @typeParam C - Config type for the widget being rendered.
 * @typeParam F - Field options type for the widget being rendered.
 *
 * @internal
 */
export interface WidgetRendererProps<C extends object = any, F extends object = any> {
    id: string
    type: string
    path?: any
    data?: any
    defaults: WidgetConfig
    optionConfig: Partial<C>
    fieldConfig: Partial<F>
    onOptionChange?: (options: Partial<C>) => void
    onFieldChange?: (options: Partial<F>) => void
    onLayoutOrderChange?: (oldIndex: number, newIndex: number) => void
    onLayoutChildrenChange?: (widgets: any, payload: Record<string, any>) => void
    onLayoutCurrentChange?: (widgets: any, payload: Record<string, any>) => void
    eventBus: EventBus
    // onFieldConfigChange?: (config: FieldConfigSource<F>) => void
    // fieldConfig?: FieldConfigSource<Partial<F>>
    // timeZone?: string
    width: number
    height: number
    children?: React.ReactNode
}

export type WidgetRendererType<C extends object = any, F extends object = any> = React.ComponentType<
    WidgetRendererProps<C, F>
>

// -----------WidgetGroup---------------

type WidgetGroupType = 'panel' | 'layout' | string

type PanelTableProps = WidgetBaseConfig

// export type WidgetState = Record<string, unknown>
// export interface WidgetBuilder<
//   T extends WidgetProps,
//   S extends WidgetState
// > {
//   buildWidget(widgetProps: T): JSX.Element;
// }
// type ISection = {
// 	id: string // section-xfasddf
// 	name: string,
// 	type: 'section'
// 	isOpen: boolean,
//   // is expaned
// 	isExpaned: boolean,
//   // section order
// 	order: number,
// 	panels: IPanel[]
// }

// type ILayout = {}
// type ILayoutGrid = {
// 	// layout
// 	type: 'grid' | 'draggble-layout'
// 	custom: true,
// 	layoutConfig: {
// 		gutter: number,
// 		columnsPerPage: number,
// 		rowsPerPage: number,
// 		boxWidth: number,
// 		heightWidth: number,
// 	}
// }

// type IPanel = {
// 	type: 'data table' | 'line chart' | 'heatmap',
// 	id: '',
// 	name: '',
// 	description: '',
// 	dataStore: [
// 		{ tableName, records}
// 	],
// 	title: ''
// }

// type IPanelPlugin = {

// }

// type IDataSource = {

// }
