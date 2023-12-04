import { useHistory, useParams } from 'react-router-dom'
import { useEventCallback } from '@starwhale/core'

export const useOnlineEvalConfig = () => {
    const { projectId, spaceId } = useParams<{
        projectId: any
        spaceId: any
    }>()
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
