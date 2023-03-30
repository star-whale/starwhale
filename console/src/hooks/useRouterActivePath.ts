import _ from 'lodash'
import { useMemo } from 'react'
import { useLocation } from 'react-router-dom'

export function useRouterActivePath(navItems: any[]) {
    const location = useLocation()
    const $item = useMemo(() => {
        const $items = navItems
            .slice()
            .reverse()
            .map((item_) => ({
                ...item_,
                url: new URL(`http://prefix${item_?.path}`),
            }))
        const tmp = $items.find((item_) => _.startsWith(location.pathname, item_.url.pathname)) ?? $items[0]
        return tmp
    }, [location.pathname, navItems])

    const paths = useMemo(() => {
        return $item?.url.pathname.split('/')
    }, [$item])

    return {
        item: $item,
        activeItemPath: $item.path,
        activeItemId: paths[paths.length - 1],
    }
}
