import _ from 'lodash'
import qs from 'qs'
import { useCallback, useMemo, useRef } from 'react'
import { useHistory, useLocation } from 'react-router-dom'

export type IQueryArgs = Record<string, any>
export type IUpdateQueryArgs = (query: Record<string, any>) => void

export const useQueryArgs = (): {
    query: IQueryArgs
    updateQuery: IUpdateQueryArgs
} => {
    const location = useLocation()
    const history = useHistory()
    const query = useMemo(() => qs.parse(location.search, { ignoreQueryPrefix: true }), [location.search])
    const queryRef = useRef(query)

    const updateQuery = useCallback(
        (newQuery: Record<string, string | number | undefined>) => {
            queryRef.current = {
                ...queryRef.current,
                ...Object.keys(newQuery).reduce((p, c) => {
                    const v = newQuery[c]
                    return {
                        ...p,
                        [c]: v === undefined ? undefined : v,
                    }
                }, {}),
            }
            history.push({
                pathname: location.pathname,
                search: qs.stringify(queryRef.current, { addQueryPrefix: true }),
            })
        },
        [history, location.pathname]
    )

    return {
        query: useMemo(
            () =>
                Object.keys(query).reduce((p, c) => {
                    const v = query[c]
                    if (v === undefined) {
                        return p
                    }
                    return {
                        ...p,
                        [c]: v,
                    }
                }, {}),
            [query]
        ),
        updateQuery,
    }
}
