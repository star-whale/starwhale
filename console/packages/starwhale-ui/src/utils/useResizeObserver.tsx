import React from 'react'
import ResizeObserver from 'resize-observer-polyfill'

// @ts-ignore
export function useResizeObserver(
    ref: { current: HTMLElement | null },
    callback: (entires: ResizeObserverEntry[], obs: ResizeObserver) => any
) {
    React.useLayoutEffect(() => {
        if (ref.current) {
            const observer = new ResizeObserver(callback)
            observer.observe(ref.current)
            return () => observer.disconnect()
        }
        // @eslint-disable-next-line consistent-return
        return () => {}
    }, [ref, callback])
}
