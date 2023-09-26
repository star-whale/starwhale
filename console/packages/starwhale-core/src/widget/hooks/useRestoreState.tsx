import _ from 'lodash'
import { PANEL_DYNAMIC_MATCHES, replacer } from '../utils/replacer'
import { useDeepEffect } from '../../utils/useDeepEffects'
import { tranformState } from '../utils'
import React from 'react'
import produce from 'immer'
import { SYNCKESY, useStore, useStoreApi } from '@starwhale/core/store'
import shallow from 'zustand/shallow'

const selector = (s: any) => ({
    initialState: s.initialState,
})

function findAllTreeNodeIds(tree: any) {
    if (!tree) return []
    let result = [tree.id]
    const children = tree?.children
    if (children) {
        for (let i = 0; i < children.length; i++) {
            result = [...result, ...findAllTreeNodeIds(children[i])]
        }
    }
    return result
}

export default function useRestoreState(dynamicVars: Record<string, any>) {
    const { initialState } = useStore(selector, shallow)
    const store = useStoreApi()

    const toSave = React.useCallback(() => {
        let data = store.getState().getRawConfigs()
        const ids = findAllTreeNodeIds(data?.tree[0])

        Object.keys(data?.widgets).forEach((id) => {
            data = produce(data, (temp) => {
                if (!ids.includes(id)) {
                    // eslint-disable-next-line
                    delete temp.widgets[id]
                }
                _.set(temp.widgets, id, replacer(PANEL_DYNAMIC_MATCHES).toTemplate(temp.widgets[id]))
            })
        })

        return data
    }, [store])

    // use  api store
    const inited = React.useRef(false)
    useDeepEffect(() => {
        if (!initialState) return
        if (inited.current) return

        // @FIXME check this
        // const novalidVars = PANEL_DYNAMIC_MATCHES.find((match) => !(match.injectKey in dynamicVars))
        // if (novalidVars) {
        //     // eslint-disable-next-line no-console
        //     // console.warn('missing vars', novalidVars)
        //     return
        // }

        try {
            let data = _.pick(typeof initialState === 'string' ? JSON.parse(initialState) : initialState, SYNCKESY)

            // for origin data
            const isOrigin = !_.get(data, 'tree.0.id')
            if (isOrigin) data = tranformState(data as any)

            Object.keys(data?.widgets).forEach((id) => {
                const origin = replacer(PANEL_DYNAMIC_MATCHES).toOrigin(data.widgets[id], dynamicVars)
                _.set(data.widgets, id, origin)
            })

            store.getState().setRawConfigs(data)
            inited.current = true
        } catch (e) {
            // eslint-disable-next-line no-console
            console.error(e)
        }
    }, [initialState, dynamicVars])

    return {
        toSave,
    }
}
