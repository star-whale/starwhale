import { useEffect } from 'react'
import useGlobalState from '@/hooks/global'
import  { ThemeType } from '@/theme'
export const useThemeType = () => {
    const [themeType, setThemeType_] = useGlobalState('themeType')

    const key = 'theme-type'

    const setThemeType = (themeType_: ThemeType) => {
        window.localStorage.setItem(key, themeType_)
        setThemeType_(themeType_)
    }

    useEffect(() => {
        const v = window.localStorage.getItem(key)
        if (v) {
            setThemeType_(v as ThemeType)
        }
    }, [setThemeType_])

    return {
        themeType,
        setThemeType,
    }
}
