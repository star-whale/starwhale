import React, { useEffect, useRef } from 'react'
import axios from 'axios'
import { toaster } from 'baseui/toast'
import { getErrMsg, setToken } from '@/api'
import { useLocation } from 'react-router-dom'
import useTranslation from '@/hooks/useTranslation'
import { useFirstRender } from '@/hooks/useFirstRender'
import { cliMateServer } from '@/consts'

function ApiHeaderSimple() {
    const location = useLocation()
    const errMsgExpireTimeSeconds = 5
    const lastErrMsgRef = useRef<Record<string, number>>({})
    const lastLocationPathRef = useRef(location.pathname)
    const [t] = useTranslation()

    useFirstRender(() => {
        // @ts-ignore
        if (axios.interceptors.response.handlers.length > 1) return

        axios.interceptors.response.use(
            (response) => {
                return response
            },
            (error) => {
                const reqUrl = error.config.url
                // ignore cli mate detection error
                if (reqUrl?.includes(`${cliMateServer}/alive`)) return Promise.reject(error)

                // for example: user/current as default token auth, it will be triggered multi times, so silent here
                const withSilentRoute = error.response.config.params?.silent
                if (withSilentRoute) return Promise.reject(error)

                const toastProps = {
                    autoHideDuration: (errMsgExpireTimeSeconds + 1) * 1000,
                    overrides: {
                        InnerContainer: {
                            style: { wordBreak: 'break-word', width: '100%' },
                        },
                    },
                    key: 'api-header',
                }
                // bad request
                if (error.response?.status === 400) {
                    const errMsg = getErrMsg(error)
                    if (errMsg) {
                        toaster.negative(errMsg, toastProps)
                    }
                    return Promise.reject(error)
                }

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
                        toastProps
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

    return <></>
}

export default ApiHeaderSimple
