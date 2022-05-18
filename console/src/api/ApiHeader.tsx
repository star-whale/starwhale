import React, { useEffect, useRef } from 'react'
import { fetchCurrentUser } from '@user/services/user'
import { useQuery } from 'react-query'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import axios from 'axios'
import { toaster } from 'baseui/toast'
import { getErrMsg, setToken } from '@/api'
import qs from 'qs'
import { useLocation } from 'react-router-dom'

export default function ApiHeader() {
    const location = useLocation()
    const errMsgExpireTimeSeconds = 5
    const lastErrMsgRef = useRef<Record<string, number>>({})
    const lastLocationPathRef = useRef(location.pathname)
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const { currentUser, setCurrentUser } = useCurrentUser()
    const userInfo = useQuery('currentUser', fetchCurrentUser, { enabled: false })

    useEffect(() => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        if ((axios.interceptors.response as any).handlers.length > 0) {
            return
        }
        axios.interceptors.response.use(
            (response) => {
                if (response.headers.authorization) setToken(response.headers.authorization)
                return response.data?.data ? response.data : response
            },
            (error) => {
                const errMsg = getErrMsg(error)
                if (error.response?.status === 403 && error.config.method === 'get') {
                    const search = qs.parse(location.search, { ignoreQueryPrefix: true })
                    let { redirect } = search
                    if (redirect && typeof redirect === 'string') {
                        redirect = decodeURI(redirect)
                    } else if (['/login', '/logout'].indexOf(location.pathname) < 0) {
                        redirect = `${location.pathname}${location.search}`
                    } else {
                        redirect = '/'
                    }
                    if (location.pathname !== '/login' && location.pathname !== '/login/') {
                        window.location.href = `${window.location.protocol}//${
                            window.location.host
                        }/login?redirect=${encodeURIComponent(redirect)}`
                    }
                } else if (Date.now() - (lastErrMsgRef.current[errMsg] || 0) > errMsgExpireTimeSeconds * 1000) {
                    toaster.negative(errMsg, { autoHideDuration: (errMsgExpireTimeSeconds + 1) * 1000 })
                    lastErrMsgRef.current[errMsg] = Date.now()
                }
                return Promise.reject(error)
            }
        )
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    useEffect(() => {
        if (userInfo.isSuccess) {
            setCurrentUser(userInfo.data)
        }
    }, [userInfo.data, userInfo.isSuccess, setCurrentUser])

    useEffect(() => {
        if (location.pathname !== '/login' && location.pathname !== '/login/' && !currentUser) {
            userInfo.refetch()
        }
    }, [userInfo, location.pathname, currentUser])

    useEffect(() => {
        if (lastLocationPathRef.current !== location.pathname) {
            lastErrMsgRef.current = {}
        }
        lastLocationPathRef.current = location.pathname
    }, [location.pathname])

    return <></>
}
