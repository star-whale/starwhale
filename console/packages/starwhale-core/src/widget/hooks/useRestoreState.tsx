import _ from 'lodash'
import { PANEL_DYNAMIC_MATCHES, replacer } from '../utils/replacer'
import { useDeepEffect } from '../../utils/useDeepEffects'
import { tranformState } from '../utils'
import { StoreType } from '@starwhale/core/context'
import React from 'react'
import produce from 'immer'

// const initialState = {
//     key: 'widgets',
//     tree: [
//         {
//             type: 'ui:dndList',
//             children: [
//                 {
//                     type: 'ui:section',
//                 },
//             ],
//         },
//     ],
//     widgets: {},
//     defaults: {},
// }

export default function useRestoreState<T>(store: StoreType, initialState: T, dynamicVars: Record<string, any>) {
    const toSave = React.useCallback(() => {
        store.setState({
            time: Date.now(),
        })
        let data = store.getState()

        Object.keys(data?.widgets).forEach((id) => {
            data = produce(data, (temp) => {
                _.set(temp.widgets, id, replacer(PANEL_DYNAMIC_MATCHES).toTemplate(temp.widgets[id]))
            })
        })

        return data
    }, [store])

    // use  api store
    useDeepEffect(() => {
        if (!initialState) return
        const novalidVars = PANEL_DYNAMIC_MATCHES.find((match) => !(match.injectKey in dynamicVars))
        if (novalidVars) {
            // eslint-disable-next-line no-console
            console.error('missing vars', novalidVars)
            return
        }

        try {
            let data = typeof initialState === 'string' ? JSON.parse(initialState) : initialState

            // for origin data
            const isOrigin = !_.get(data, 'tree.0.id')
            if (isOrigin) data = tranformState(data)

            Object.keys(data?.widgets).forEach((id) => {
                _.set(data.widgets, id, replacer(PANEL_DYNAMIC_MATCHES).toOrigin(data.widgets[id], dynamicVars))
            })

            if (store.getState().time >= data?.time) {
                // eslint-disable-next-line no-console
                console.error('time is not valid', store.getState().time, data?.time)
                return
            }

            store.setState(data)
        } catch (e) {
            // eslint-disable-next-line no-console
            console.error(e)
        }
    }, [initialState, dynamicVars])

    return {
        toSave,
    }
}
