import { ISidebarContextProps } from '@/contexts/SidebarContext'
import { useLocalStorage } from 'react-use'

export const useSidebar = (): ISidebarContextProps => {
    const [expanded, setExpanded] = useLocalStorage('siderExpanded', true)

    return {
        expanded,
        setExpanded,
    }
}
