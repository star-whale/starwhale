import _ from 'lodash'
import { useMemo } from 'react'
import { useLocation } from 'react-router-dom'

export function useRouterActivePath(navItems: any[]) {
    const location = useLocation()
    const $item = useMemo(() => {
        const item = navItems
            .slice()
            .reverse()
            .map((item_) => ({
                ...item_,
                url: new URL(`http://prefix${item_?.path}`),
            }))
            .find((item_) => _.startsWith(location.pathname, item_.url.pathname))
        return item
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
