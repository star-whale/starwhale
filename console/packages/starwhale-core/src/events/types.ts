// eslint-disable-next-line max-classes-per-file
import { Unsubscribable, Observable } from 'rxjs'

/**
 * @alpha
 * internal interface
 */
export interface BusEvent {
    readonly type: string
    readonly payload?: any
    readonly origin?: EventBus
}

/**
 * @alpha
 * Base event type
 */
export abstract class BusEventBase implements BusEvent {
    readonly type: string

    readonly payload?: any

    readonly origin?: EventBus

    constructor() {
        // @ts-ignore
        // eslint-disable-next-line
        this.type = this.__proto__.constructor.type
    }
}

/**
 * @alpha
 * Base event type with payload
 */
export abstract class BusEventWithPayload<T> extends BusEventBase {
    readonly payload: T

    constructor(payload: T) {
        super()
        this.payload = payload
    }
}

/*
 * Interface for an event type constructor
 */
export interface BusEventType<T extends BusEvent> {
    type: string
    new (...args: any[]): T
}

/**
 * @alpha
 * Event callback/handler type
 */
export interface BusEventHandler<T extends BusEvent> {
    (event: T): void
}

/**
 * @alpha
 * Main minimal interface
 */
export interface EventFilterOptions {
    onlyLocal: boolean
}

/**
 * @alpha
 * Main minimal interface
 */
export interface EventBus {
    /**
     * Publish single event
     */
    publish<T extends BusEvent>(event: T): void

    /**
     * Get observable of events
     */
    getStream<T extends BusEvent>(eventType: BusEventType<T>): Observable<T>

    /**
     * Subscribe to an event stream
     *
     * This function is a wrapper around the `getStream(...)` function
     */
    subscribe<T extends BusEvent>(eventType: BusEventType<T>, handler: BusEventHandler<T>): Unsubscribable

    /**
     * Remove all event subscriptions
     */
    removeAllListeners(): void

    /**
     * Returns a new bus scoped that knows where it exists in a heiarchy
     *
     * @internal -- This is included for internal use only should not be used directly
     */
    newScopedBus(key: string, filter: EventFilterOptions): EventBus
}

/** @alpha */
export type EventBusExtended = EventBus
