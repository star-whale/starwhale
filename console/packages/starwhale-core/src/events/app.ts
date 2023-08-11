// eslint-disable-next-line max-classes-per-file
import { BusEventWithPayload, BusEventBase } from './types'

type PanelAddPayload = { path: any[] }
export class PanelAddEvent extends BusEventWithPayload<PanelAddPayload> {
    static type = 'add-panel'
}

type PanelEditPayload = { id: string; evalSelectData?: any }
export class PanelEditEvent extends BusEventWithPayload<PanelEditPayload> {
    static type = 'edit-panel'
}

type PanelDeletePayload = { id: string }
export class PanelDeleteEvent extends BusEventWithPayload<PanelDeletePayload> {
    static type = 'delete-panel'
}

type PanelPreviewPayload = { id: string }
export class PanelPreviewEvent extends BusEventWithPayload<PanelPreviewPayload> {
    static type = 'preview-panel'
}

type PanelDownloadPayload = { id: string }
export class PanelDownloadEvent extends BusEventWithPayload<PanelDownloadPayload> {
    static type = 'download-panel'
}

type PanelReloadPayload = { id: string }
export class PanelReloadEvent extends BusEventWithPayload<PanelReloadPayload> {
    static type = 'reload-panel'
}

type SectionAddPayload = { path: any[]; type: string }
export class SectionAddEvent extends BusEventWithPayload<SectionAddPayload> {
    static type = 'add-section'
}

export class PanelSaveEvent extends BusEventBase {
    static type = 'save'
}

export type SectionEvalSelectDataPayload = Record<
    string,
    {
        summaryTableName: string
        projectId: string
        records: any[]
        columnTypes: any[]
        currentView: any
        rowSelectedIds: string[]
        project: any
    }
>
export class SectionEvalSelectDataEvent extends BusEventWithPayload<SectionEvalSelectDataPayload> {
    static type = 'eval-section'
}
