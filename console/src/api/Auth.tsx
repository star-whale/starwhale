import { ILoginUserSchema } from '@/domain/user/schemas/user'
import { fetchCurrentUser, loginUser } from '@/domain/user/services/user'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import React, { useEffect } from 'react'
import { useQuery } from 'react-query'
import qs from 'qs'
import { getToken } from '.'

type IAuthContext = {
    token: string | null
    onLogin: (data: ILoginUserSchema) => string
    onLoginOut: () => void
}
export const AuthContext = React.createContext<IAuthContext>({ token: null, onLogin: () => '', onLoginOut: () => {} })

export const useAuth = () => {
    return React.useContext(AuthContext)
}

// eslint-disable-next-line
const location = window.location

export const AuthProvider = ({ children }: any) => {
    const [token, setToken] = React.useState(getToken())

    const userInfo = useQuery('currentUser', fetchCurrentUser)

    const { currentUser, setCurrentUser } = useCurrentUser()

    useEffect(() => {
        if (userInfo.isSuccess) {
            setCurrentUser(userInfo.data)
            if (userInfo.data) {
                setToken(getToken())
            }
        }
    }, [userInfo, setCurrentUser])

    const handleLogin = async (data: ILoginUserSchema) => {
        await loginUser(data)
        await userInfo.refetch()

        const search = qs.parse(location.search, { ignoreQueryPrefix: true })
        let { redirect } = search
        if (redirect && typeof redirect === 'string') {
            redirect = decodeURI(redirect)
        } else {
            redirect = '/'
        }

        setToken(getToken())
        return redirect
    }

    const handleLogout = () => {
        setToken(null)
    }

    console.log('raw:', !!getToken(), 'new:', !!token, currentUser)

    const value = {
        token,
        onLogin: handleLogin,
        onLogout: handleLogout,
    }

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
