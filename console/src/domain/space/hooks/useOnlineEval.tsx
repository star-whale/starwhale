import React, { useEffect } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import { useEventCallback } from '@starwhale/core'
import useTranslation from '@/hooks/useTranslation'

export const useOnlineEvalConfig = () => {
    const { projectId, spaceId } = useParams<{
        projectId: any
        spaceId: any
    }>()
    const [t] = useTranslation()
    const history = useHistory()

    const routes = {
        onlines: `/projects/${projectId}/spaces/${spaceId}/fine-tune-onlines`,
        onlineServings: `/projects/${projectId}/spaces/${spaceId}/fine-tune-online-servings`,
    }
    const gotoList = useEventCallback(() => {
        history.push(`/projects/${projectId}/spaces/${spaceId}/fine-tune-evals`)
    })

    return {
        projectId,
        spaceId,
        gotoList,
        routes,
    }
}

export default useOnlineEvalConfig
