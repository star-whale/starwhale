import React, { useEffect, useRef } from 'react'
import axios from 'axios'
import { toaster } from 'baseui/toast'
import { getErrMsg, setToken } from '@/api'
import { useLocation } from 'react-router-dom'
import useTranslation from '@/hooks/useTranslation'
import { useFirstRender } from '@/hooks/useFirstRender'
import { useProject } from '@project/hooks/useProject'
import { useFetchProject } from '@/domain/project/hooks/useFetchProject'
import qs from 'qs'
import { useProjectRole } from '@/domain/project/hooks/useProjectRole'
import { useFetchProjectRole } from '@/domain/project/hooks/useFetchProjectRole'
import { cliMateServer } from '@/consts'

function ApiHeader() {
    const location = useLocation()
    const errMsgExpireTimeSeconds = 5
    const lastErrMsgRef = useRef<Record<string, number>>({})
    const lastLocationPathRef = useRef(location.pathname)
    const [t] = useTranslation()
    const projectId = React.useMemo(() => location?.pathname.match(/^\/projects\/(.+?)\/.*/)?.[1], [location])
    const projectInfo = useFetchProject(projectId)
    const { setProject } = useProject()
    const { role: projectRole } = useFetchProjectRole(projectId as string)
    const { setRole } = useProjectRole()

    useFirstRender(() => {
        // @ts-ignore
        if (axios.interceptors.response.handlers.length > 0) return

        axios.interceptors.response.use(
            (response) => {
                if (response.headers?.authorization) setToken(response.headers.authorization)
                return typeof response.data === 'object' && 'data' in response.data ? response.data : response
            },
            (error) => {
                // eslint-disable-next-line no-restricted-globals
                // eslint-disable-next-line prefer-destructuring
                const winLocation = window.location

                const reqUrl = error.config.url
                // ignore cli mate detection error
                if (reqUrl?.includes(`${cliMateServer}/alive`)) return Promise.reject(error)

                if (error.response?.status === 401) {
                    setToken(undefined)
                }

                if (error.response?.status === 401 && error.config.method === 'get') {
                    const withUnAuthRoute =
                        ['/login', '/signup', '/create-account', '/reset'].filter((path) =>
                            winLocation.pathname.includes(path)
                        ).length > 0
                    const search = qs.parse(winLocation.search, { ignoreQueryPrefix: true })
                    let { redirect } = search
                    if (redirect && typeof redirect === 'string') {
                        redirect = decodeURI(redirect)
                    } else if (!withUnAuthRoute) {
                        redirect = `${winLocation.pathname}${winLocation.search}`
                    } else {
                        redirect = '/projects'
                    }

                    if (!withUnAuthRoute) {
                        winLocation.href = `${winLocation.protocol}//${
                            winLocation.host
                        }/login?redirect=${encodeURIComponent(redirect)}`
                    }
                }

                // ignore `bad gateway` error in online eval
                // `gateway/*` is for online eval, see:
                // - https://github.com/star-whale/starwhale/blob/main/server/controller/src/main/java/ai/starwhale/mlops/configuration/ProxyServletConfiguration.java#L28
                // - https://github.com/star-whale/starwhale/blob/main/server/controller/src/main/java/ai/starwhale/mlops/domain/job/ModelServingService.java#L95-L96
                if (error.response?.status === 502 && winLocation.pathname.includes('online_eval')) {
                    return Promise.reject(error)
                }

                // for example: user/current as default token auth, it will be triggered multi times, so silent here
                const withSilentRoute = error.response.config.params.silent
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
                            key: 'api-header',
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
        if (lastLocationPathRef.current !== location.pathname) {
            lastErrMsgRef.current = {}
        }
        lastLocationPathRef.current = location.pathname
    }, [location.pathname])

    useEffect(() => {
        if (projectInfo.data) {
            setProject(projectInfo.data)
        }
    }, [projectInfo.data, setProject, projectId])

    useEffect(() => {
        if (projectRole) {
            setRole(projectRole)
        }
    }, [projectRole, setRole])

    return <></>
}

export default ApiHeader
