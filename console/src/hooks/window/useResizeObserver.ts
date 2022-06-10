import { useLayoutEffect, RefObject } from 'react'

const useResizeObserver = (
    resizeObserverCallback: ResizeObserverCallback,
    target: RefObject<HTMLElement>,
    returnCallback?: () => void
): void => {
    useLayoutEffect(() => {
        if (target?.current) {
            const observer = new window.ResizeObserver(resizeObserverCallback)

            if (observer) {
                observer.observe(target.current)
            }
            return () => {
                if (observer) {
                    observer.disconnect()
                    if (typeof returnCallback === 'function') {
                        returnCallback()
                    }
                }
            }
        }
        return () => {}
    }, [resizeObserverCallback, returnCallback, target])
}

export default useResizeObserver
