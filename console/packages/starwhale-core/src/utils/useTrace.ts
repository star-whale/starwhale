import { useEffect, useRef } from 'react'
import { isDebug } from './debug'
import _ from 'lodash'
import { useUID } from 'react-uid'

function getRandomColor() {
    const letters = '3333456789ABCDEF'
    let color = '#'
    for (let i = 0; i < 6; i++) {
        color += letters[Math.floor(Math.random() * 16)]
    }
    return color
}

export const trace = (...args: any[]) => {
    if (isDebug()) {
        // eslint-disable-next-line
        console.log(...args.map((a) => (_.isFunction(a) ? a() : a)))
    }
}
export const traceCreator = (key: string, id = '0', color = getRandomColor()) => {
    return (...args: any[]) =>
        trace(`%c[${key}-${id}]:`, `background: ${color}; color: black; font-weight: bold;`, ...args)
}

export function useTrace(key) {
    const ref = useRef<typeof trace>(() => {})
    const id = useUID()

    useEffect(() => {
        ref.current = traceCreator(key, id)
    }, [key, id])

    return ref.current
}
