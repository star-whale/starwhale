import { isEqual } from 'lodash'
import { DependencyList, EffectCallback, useEffect, useRef } from 'react'

export function useDeepEffect(func: EffectCallback, deps: DependencyList) {
    const isFirst = useRef(true)
    const prevDeps = useRef(deps)

    useEffect(() => {
        const isSame = prevDeps.current.every((obj, idx) => isEqual(obj, deps[idx]))

        if (isFirst.current || !isSame) {
            func()
        }

        isFirst.current = false
        prevDeps.current = deps
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, deps)
}
