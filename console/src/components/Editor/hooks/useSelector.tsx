import { isEqual } from 'lodash'
import React from 'react'
import { useStore } from 'zustand'
import { useEditorContext } from '../context/EditorContextProvider'
import { WidgetStoreState } from '../context/store'

export const getTree = (state: WidgetStoreState) => state.tree
export const getWidget = (id: string) => (state: WidgetStoreState) => {
    return state.widgets?.[id]
}
export const getWidgetDefaults = (type: string) => (state: WidgetStoreState) => state.defaults?.[type]

export default function useSelector(selector) {
    const { store } = useEditorContext()
    return store(selector, isEqual)
}
