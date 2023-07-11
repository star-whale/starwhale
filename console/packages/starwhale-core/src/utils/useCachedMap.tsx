import React from 'react'
import { useLocalStorage } from 'react-use'

const defaultMap = {}
const defaultKeys: any[] = []

function useCachedMap(storeKey = 'cache', maxCacheSize = 100) {
    const [cachedMap, setCachedMap] = useLocalStorage<Record<string, DOMRect>>(`cache-${storeKey}`, defaultMap)
    const [cachedKeys, setCachedKeys] = useLocalStorage<string[]>(`cache-keys-${storeKey}`, defaultKeys)

    const has = React.useCallback(
        (key: string) => {
            return cachedMap?.[key] !== undefined
        },
        [cachedMap]
    )
    const get = React.useCallback(
        (key: string) => {
            return cachedMap?.[key]
        },
        [cachedMap]
    )
    const put = React.useCallback(
        (key: string, value: DOMRect) => {
            if (!cachedMap || !cachedKeys) return

            cachedMap[key] = value
            if (cachedKeys.length > maxCacheSize) {
                const first = cachedKeys.shift()
                if (first) delete cachedMap[first]
            } else {
                cachedKeys.push(key)
            }

            setCachedMap({ ...cachedMap })
            setCachedKeys([...cachedKeys])
        },
        [cachedMap, cachedKeys, maxCacheSize, setCachedMap, setCachedKeys]
    )

    return React.useMemo(() => {
        return {
            has,
            get,
            put,
        }
    }, [has, get, put])
}

export { useCachedMap }
export default useCachedMap
