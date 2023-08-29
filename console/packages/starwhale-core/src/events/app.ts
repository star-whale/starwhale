// eslint-disable-next-line max-classes-per-file
import { BusEventWithPayload, BusEventBase } from './types'

type PanelChartAddPayload = { path: any[] }
export class PanelChartAddEvent extends BusEventWithPayload<PanelChartAddPayload> {
    static type = 'panel-chart-add'
}

type PanelChartEditPayload = { id: string; evalSelectData?: any }
export class PanelChartEditEvent extends BusEventWithPayload<PanelChartEditPayload> {
    static type = 'panel-chart-edit'
}

type PanelChartDeletePayload = { id: string }
export class PanelChartDeleteEvent extends BusEventWithPayload<PanelChartDeletePayload> {
    static type = 'panel-chart-delete'
}

type PanelChartPreviewPayload = { id: string }
export class PanelChartPreviewEvent extends BusEventWithPayload<PanelChartPreviewPayload> {
    static type = 'panel-chart-preview'
}

type PanelChartDownloadPayload = { id: string }
export class PanelChartDownloadEvent extends BusEventWithPayload<PanelChartDownloadPayload> {
    static type = 'panel-chart-download'
}

type PanelChartReloadPayload = { id: string }
export class PanelChartReloadEvent extends BusEventWithPayload<PanelChartReloadPayload> {
    static type = 'panel-chart-reload'
}

type SectionAddPayload = { path: any[]; type: string }
export class SectionAddEvent extends BusEventWithPayload<SectionAddPayload> {
    static type = 'add-section'
}

type EvalSectionDeletePayload = { id: string }
export class EvalSectionDeleteEvent extends BusEventWithPayload<EvalSectionDeletePayload> {
    static type = 'delete-eval-section'
}

export class PanelChartSaveEvent extends BusEventBase {
    static type = 'panel-chart-save'
}

export type SectionEvalSelectDataPayload = Record<
    string,
    {
        summaryTableName: string
        projectId: string
        records: any[]
        columnTypes: any[]
        project: any
    }
>
export class SectionEvalSelectDataEvent extends BusEventWithPayload<SectionEvalSelectDataPayload> {
    static type = 'eval-section'
}
