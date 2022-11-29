import { BusEventBase, BusEventWithPayload } from './types'

/**
 * When hovering over an element this will identify
 *
 * For performance reasons, this object will usually be mutated between updates.  This
 * will avoid creating new objects for events that fire frequently (ie each mouse pixel)
 *
 */
export interface DataHoverPayload {
    rowIndex?: number // the hover row
    columnIndex?: number // the hover column
    dataId?: string // identifying string to correlate data between publishers and subscribers

    // When dragging, this will capture the point when the mouse was down
    point: Record<string, any> // { time: 5678, lengthft: 456 }  // each axis|scale gets a value
    down?: Record<string, any>
}

export class DataHoverEvent extends BusEventWithPayload<DataHoverPayload> {
    static type = 'data-hover'
}

export class DataHoverClearEvent extends BusEventBase {
    static type = 'data-hover-clear'
}

export class DataSelectEvent extends BusEventWithPayload<DataHoverPayload> {
    static type = 'data-select'
}

export class DragStartEvent extends BusEventBase {
    static type = 'drag-start'
}

export class DragEndEvent extends BusEventBase {
    static type = 'drag-end'
}
