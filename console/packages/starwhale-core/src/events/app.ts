import { BusEventWithPayload, BusEvent, BusEventBase } from './types'

type PanelAddPayload = { path: any[] }
export class PanelAddEvent extends BusEventWithPayload<PanelAddPayload> {
    static type = 'add-panel'
}

type PanelEditPayload = { id: string }
export class PanelEditEvent extends BusEventWithPayload<PanelEditPayload> {
    static type = 'edit-panel'
}

type SectionAddPayload = { path: any[]; type: string }
export class SectionAddEvent extends BusEventWithPayload<SectionAddPayload> {
    static type = 'add-section'
}

export class PanelSaveEvent extends BusEventBase {
    static type = 'save'
}
