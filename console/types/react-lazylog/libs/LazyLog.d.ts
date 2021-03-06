/* eslint-disable react/static-property-placement */
/* eslint-disable react/prefer-stateless-function */
/* eslint-disable import/no-extraneous-dependencies */
/* eslint-disable @typescript-eslint/naming-convention */
/* eslint-disable @typescript-eslint/no-explicit-any */
import { Component, ReactNode, CSSProperties } from 'react'
import { Range } from 'immutable'

export interface WebsocketOptions {
    onOpen?: (e: Event, socket: WebSocket) => void
    onClose?: (e: CloseEvent) => void
    onError?: (e: Event) => void
    formatMessage?: (message: any) => string
}

export interface LazyLogProps {
    caseInsensitive?: boolean
    containerStyle?: CSSProperties
    enableSearch?: boolean
    extraLines?: number
    fetchOptions?: RequestInit
    follow?: boolean
    formatPart?: (text: string) => ReactNode
    height?: string | number
    highlight?: number | number[]
    highlightLineClassName?: string
    lineClassName?: string
    loadingComponent?: any
    onError?: (error: any) => any
    onHighlight?: (range: Range) => any
    onLoad?: () => any
    overscanRowCount?: number
    rowHeight?: number
    scrollToLine?: number
    selectableLines?: boolean
    stream?: boolean
    style?: CSSProperties
    text?: string
    url: string
    websocket?: boolean
    websocketOptions?: WebsocketOptions
    width?: string | number
    onScroll: (params: { clientHeight: number; scrollHeight: number; scrollTop: number }) => void
}

export class LazyLog extends Component<LazyLogProps> {
    static defaultProps: Partial<LazyLogProps>
}

export default LazyLog
