import { useEffect, useRef } from 'react'

import { IGridState } from '../types'
import useGrid from './useGrid'

function useOnInitHandler(onInit?: (state: Partial<IGridState>) => void | undefined) {
    const rfInstance = useGrid()
    const isInitialized = useRef<boolean>(false)

    useEffect(() => {
        if (!isInitialized.current && onInit) {
            setTimeout(() => onInit(rfInstance as any), 1)
            isInitialized.current = true
        }
    }, [onInit, rfInstance])
}

export default useOnInitHandler
