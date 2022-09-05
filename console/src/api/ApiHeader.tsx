import React, { useEffect, useRef } from 'react'
import { fetchCurrentUserRoles } from '@user/services/user'
import { useQuery } from 'react-query'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import axios from 'axios'
import { toaster } from 'baseui/toast'
import { getErrMsg, setToken } from '@/api'
import { useLocation } from 'react-router-dom'
import useTranslation from '@/hooks/useTranslation'
import { useCurrentUserRoles } from '@/hooks/useCurrentUserRoles'
import { useFirstRender } from '@/hooks/useFirstRender'
import { useProject } from '@project/hooks/useProject'
import { useFetchProject } from '@/domain/project/hooks/useFetchProject'
import qs from 'qs'

export default function ApiHeader() {
    const location = useLocation()
    const errMsgExpireTimeSeconds = 5
    const lastErrMsgRef = useRef<Record<string, number>>({})
    const lastLocationPathRef = useRef(location.pathname)
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const { currentUser } = useCurrentUser()
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const [, setCurrentUserRoles] = useCurrentUserRoles()
    const userRoles = useQuery('currentUserRoles', () => fetchCurrentUserRoles(), { enabled: false })
    const [t] = useTranslation()
    const projectId = React.useMemo(() => location?.pathname.match(/^\/projects\/(\d*)\/?/)?.[1], [location])
    const projectInfo = useFetchProject(projectId)
    const { setProject } = useProject()

    useFirstRender(() => {
        // @ts-ignore
        if (axios.interceptors.response.handlers.length > 0) return

        axios.interceptors.response.use(
            (response) => {
                if (response.headers?.authorization) setToken(response.headers.authorization)
                return response.data?.data ? response.data : response
            },
            (error) => {
                // eslint-disable-next-line no-restricted-globals
                // eslint-disable-next-line prefer-destructuring
                if (error.response?.status === 401) {
                    setToken(undefined)
                }

                if (error.response?.status === 401 && error.config.method === 'get') {
                    const withUnAuthRoute =
                        ['/login', '/signup', '/create-account', 'logout'].filter((path) =>
                            location.pathname.includes(path)
                        ).length > 0
                    const search = qs.parse(location.search, { ignoreQueryPrefix: true })
                    let { redirect } = search
                    if (redirect && typeof redirect === 'string') {
                        redirect = decodeURI(redirect)
                    } else if (!withUnAuthRoute) {
                        redirect = `${location.pathname}${location.search}`
                    } else {
                        redirect = '/projects'
                    }

                    if (!withUnAuthRoute) {
                        location.href = `${location.protocol}//${location.host}/login?redirect=${encodeURIComponent(
                            redirect
                        )}`
                    }
                }

                // use user/current as default token auth, it will be triggered multi times, so slient here
                const withSilentRoute = error.response.config.url.includes('/user/current')
                if (withSilentRoute) return Promise.reject(error)

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
                return Promise.reject(error)
            }
        )
        // eslint-disable-next-line react-hooks/exhaustive-deps
    })

    useEffect(() => {
        if (currentUser) {
            userRoles.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentUser])

    useEffect(() => {
        if (lastLocationPathRef.current !== location.pathname) {
            lastErrMsgRef.current = {}
        }
        lastLocationPathRef.current = location.pathname
    }, [location.pathname])

    useEffect(() => {
        if (userRoles.data) {
            setCurrentUserRoles(userRoles.data)
        }
    }, [userRoles.data, setCurrentUserRoles])

    useEffect(() => {
        if (projectInfo.data) {
            setProject(projectInfo.data)
        }
    }, [projectInfo.data, setProject, projectId])

    return <></>
}
