import React, { useEffect, useRef } from 'react'
import { fetchCurrentUserRoles } from '@user/services/user'
import { useQuery } from 'react-query'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import axios from 'axios'
import { toaster } from 'baseui/toast'
import { getErrMsg } from '@/api'
import { useLocation, useParams } from 'react-router-dom'
import useTranslation from '@/hooks/useTranslation'
import { useCurrentUserRoles } from '@/hooks/useCurrentUserRoles'
import { useFirstRender } from '../hooks/useFirstRender'
import { useProject } from '@project/hooks/useProject'
import { useFetchProject } from '@/domain/project/hooks/useFetchProject'

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
    const { projectId } = useParams<{ projectId: string }>()
    const projectInfo = useFetchProject(projectId)
    const { setProject } = useProject()

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
