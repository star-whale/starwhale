import { drawerExpandedWidthOfColumnManage } from '@/consts'
import { useMemo } from 'react'
import useGlobalState from './global'

export const useDrawer = () => {
    const [expanded, setExpanded] = useGlobalState('drawerExpanded')
    // const [expandedLocal, setExpandedLocal] = useLocalStorage('drawerExpanded', true)

    // const $setExpanded = useMemo(() => {
    //     return (expanded: boolean) => {
    //         // setExpandedLocal(expanded)
    //         setExpanded(expanded)
    //     }
    // }, [setExpanded, setExpandedLocal])

    // const $expanded = useMemo(() => {
    //     return expandedLocal
    // }, [expandedLocal])

    const expandedWidth = useMemo(() => {
        if (expanded) {
            return drawerExpandedWidthOfColumnManage
        }
        return 0
    }, [expanded])

    return {
        expandedWidth,
        expanded,
        setExpanded,
    }
}
