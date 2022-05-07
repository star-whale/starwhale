import _ from 'lodash'
import React, { useState, useEffect, useRef } from 'react'

interface IWebSocket {
    wsUrl: string
    debug?: boolean
    onOpen?: () => void
    onClose?: () => void
    onMessage?: (event: Event) => void
    onError?: (event: Event) => void
}

export default function useWebSocket({ wsUrl, onOpen, onClose, onMessage, debug = false }: IWebSocket) {
    const heartBeatTimeoutRef = useRef(undefined as undefined | number)
    const wsRef = useRef(undefined as undefined | WebSocket)

    const test = () => {
        console.log('test')
    }
    // const log = debug
    //     ? _.bind(console.log, console, '[useWebSocket]')
    //     : () => {
    //           console.log('null')
    //       }

    // const log1 = _.bind(console.log, console, '[useWebSocket]')

    useEffect(() => {
        const log = _.bind(console.log, console, '[useWebSocket]')
        log('use effect', wsUrl, debug)
        // log1('use 1')
        // log2('use 2')
        test()
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
        return () => {
            log('react layout closing')
            close()
        }
    }, [wsUrl])
}
