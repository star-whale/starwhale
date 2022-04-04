import { useEffect } from 'react'
import useGlobalState, { ThemeType } from '@/hooks/global'

export const useToken = () => {
    const [token, setToken_] = useGlobalState('token')

    const key = 'token'

    const setToken = (token: string | undefined) => {
        if (!token) return window.sessionStorage.removeItem(key)
        window.sessionStorage.setItem(key, token)
        setToken_(token)
    }

    useEffect(() => {
        const v = window.sessionStorage.getItem(key)
        if (v) {
            setToken_(v)
        }
    }, [setToken])

    return {
        token,
        setToken,
    }
}
