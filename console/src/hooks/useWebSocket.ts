/* eslint-disable @typescript-eslint/no-unused-vars */
import React, { useEffect, useRef } from 'react'
import _ from 'lodash'

interface IWebSocket {
    wsUrl: string
    debug?: boolean
    onOpen?: () => void
    onClose?: () => void
    onMessage?: (event: Event) => void
    onError?: (event: Event) => void
}

export default function useWebSocket({ wsUrl, onOpen, onClose, onMessage }: IWebSocket) {
    const heartBeatTimeoutRef = useRef(undefined as undefined | number)
    const wsRef = useRef(undefined as undefined | WebSocket)
    useEffect(() => {
        // const log = _.bind(console.log, console, '[useWebSocket]')
        const log = (...args: any[]) => {}
        log('use effect', wsUrl)

        if (!wsUrl) {
            return
        }
        if (wsRef.current) {
            log('prev closed', wsRef.current)
            wsRef.current.close()
        }
        const ws = new WebSocket(wsUrl)
        wsRef.current = ws

        const heartbeat = () => {
            if (ws?.readyState === ws?.OPEN) {
                ws?.send('ping')
            }
            heartBeatTimeoutRef.current = window.setTimeout(heartbeat, 2000)
        }
        const close = () => {
            log('WebSocket closing')
            window.clearTimeout(heartBeatTimeoutRef.current)
            ws?.close()
            wsRef.current = undefined
            heartBeatTimeoutRef.current = undefined
            onClose?.()
        }

        ws.onopen = () => heartbeat()
        ws.onclose = (e) => {
            log('WebSocket closed', e)
        }
        ws.onerror = (e) => {
            log('WebSocket error', e)
        }
        ws.onmessage = (event) => {
            onMessage?.(event.data)
        }
        // eslint-disable-next-line consistent-return
        return () => {
            log('react layout closing')
            close()
        }
    }, [wsUrl, onClose, onOpen, onMessage])
}
