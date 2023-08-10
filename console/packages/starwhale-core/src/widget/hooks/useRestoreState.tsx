import _ from 'lodash'
import { PANEL_DYNAMIC_MATCHES, replacer } from '../utils/replacer'
import { useDeepEffect } from '../../utils/useDeepEffects'
import { tranformState } from '../utils'
import { StoreType } from '@starwhale/core/context'
import React from 'react'
import produce from 'immer'
import { useEffectOnce } from 'react-use'

export default function useRestoreState<T>(store: StoreType, initialState: T, dynamicVars: Record<string, any>) {
    const toSave = React.useCallback(() => {
        let data = store.getState()

        console.log(data)

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
        console.log('store inited')

        const novalidVars = PANEL_DYNAMIC_MATCHES.find((match) => !(match.injectKey in dynamicVars))
        if (novalidVars) {
            // eslint-disable-next-line no-console
            // console.warn('missing vars', novalidVars)
            // store.setState({
            //     isInit: true,
            // })
            return
        }

        try {
            let data = typeof initialState === 'string' ? JSON.parse(initialState) : initialState

            // for origin data
            const isOrigin = !_.get(data, 'tree.0.id')
            if (isOrigin) data = tranformState(data)

            Object.keys(data?.widgets).forEach((id) => {
                const origin = replacer(PANEL_DYNAMIC_MATCHES).toOrigin(data.widgets[id], dynamicVars)
                _.set(data.widgets, id, origin)
            })

            // setTimeout(() => {
            //     data.isInit = true
            //     store.setState(data)
            // }, 0)

            // store.setState(data)
        } catch (e) {
            // eslint-disable-next-line no-console
            console.error(e)
        }
    }, [initialState, dynamicVars])

    return {
        toSave,
    }
}
