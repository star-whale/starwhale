import { isEqual } from 'lodash'
import { useEditorContext } from '../../context/EditorContextProvider'
import { WidgetStoreState } from '../../types'

export const getTree = (state: WidgetStoreState) => state.tree
export const getWidget = (id: string) => (state: WidgetStoreState) => {
    return state.widgets?.[id]
}
export const getWidgetDefaults = (type: string) => (state: WidgetStoreState) => state.defaults?.[type]

// @ts-ignore
export default function useSelector(selector) {
    const { store } = useEditorContext()
    return store(selector, isEqual)
}
