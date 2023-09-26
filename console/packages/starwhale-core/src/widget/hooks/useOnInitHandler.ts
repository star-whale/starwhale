import { useEffect, useRef } from 'react'

function useOnInitHandler(onInit?: (state: any) => void | undefined, state?: any) {
    const isInitialized = useRef<boolean>(false)

    useEffect(() => {
        if (!isInitialized.current && onInit && state) {
            setTimeout(() => onInit(state as any), 1)
            isInitialized.current = true
        }
    }, [onInit, state])
}

export default useOnInitHandler
