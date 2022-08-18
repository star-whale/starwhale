import React, { useEffect, useRef } from 'react'
import { fetchCurrentUser, fetchCurrentUserRoles } from '@user/services/user'
import { useQuery } from 'react-query'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import axios from 'axios'
import { toaster } from 'baseui/toast'
import { getErrMsg, getToken } from '@/api'
import { useLocation } from 'react-router-dom'
import useTranslation from '@/hooks/useTranslation'
import { useCurrentUserRoles } from '@/hooks/useCurrentUserRoles'
import { useFirstRender } from '../hooks/useFirstRender'

export default function ApiHeader() {
    const location = useLocation()
    const errMsgExpireTimeSeconds = 5
    const lastErrMsgRef = useRef<Record<string, number>>({})
    const lastLocationPathRef = useRef(location.pathname)
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const { currentUser, setCurrentUser } = useCurrentUser()
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const [, setCurrentUserRoles] = useCurrentUserRoles()
    const userInfo = useQuery('currentUser', fetchCurrentUser, { enabled: false })
    const userRoles = useQuery('currentUserRoles', () => fetchCurrentUserRoles(), { enabled: false })
    const [t] = useTranslation()

    useFirstRender(() => {
        axios.interceptors.response.use(
            (response) => response,
            (error) => {
                const errMsg = getErrMsg(error)
                if (Date.now() - (lastErrMsgRef.current[errMsg] || 0) > errMsgExpireTimeSeconds * 1000) {
                    toaster.negative(
                        errMsg.length < 100 ? (
                            errMsg
                        ) : (
                            <>
                                <details>
                                    <summary>{t('something wrong with the server')}</summary>
                                    {errMsg}
                                </details>
                            </>
                        ),
                        {
                            autoHideDuration: (errMsgExpireTimeSeconds + 1) * 1000,
                            overrides: {
                                InnerContainer: {
                                    style: { wordBreak: 'break-word', width: '100%' },
                                },
                            },
                        }
                    )
                    lastErrMsgRef.current[errMsg] = Date.now()
                }
                return error
            }
        )
        // eslint-disable-next-line react-hooks/exhaustive-deps
    })

    useEffect(() => {
        if (userInfo.isSuccess) {
            setCurrentUser(userInfo.data)
        }
    }, [userInfo.data, userInfo.isSuccess, setCurrentUser])

    useEffect(() => {
        if (!currentUser && getToken()) {
            userInfo.refetch()
            userRoles.refetch()
        }
    }, [userInfo, currentUser, userRoles])

    useEffect(() => {
        if (lastLocationPathRef.current !== location.pathname) {
            lastErrMsgRef.current = {}
        }
        lastLocationPathRef.current = location.pathname
    }, [location.pathname])

    useEffect(() => {
        setCurrentUserRoles(userRoles.data)
    }, [userRoles.data, setCurrentUserRoles])

    return <></>
}
