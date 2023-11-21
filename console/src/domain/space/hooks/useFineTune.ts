import React from 'react'
import { api } from '@/api'
import useGlobalState from '@/hooks/global'
import { useParams } from 'react-router-dom'
import { useEventCallback } from '@starwhale/core'
import { useProject } from '@/domain/project/hooks/useProject'

export const useFineTune = () => {
    const [fineTune, setFineTune] = useGlobalState('fineTune')
    return {
        fineTune,
        setFineTune,
    }
}

export const useFineTuneLoading = () => {
    const [fineTuneLoading, setFineTuneLoading] = useGlobalState('fineTuneLoading')

    return {
        fineTuneLoading,
        setFineTuneLoading,
    }
}

export const LOCAL_VIEW_ID_KEY = 'fine-tune-view-id'

export const useFineTuneConfig = () => {
    const {
        projectId: projectFromUri,
        spaceId,
        fineTuneId,
    } = useParams<{ projectId: any; spaceId: any; fineTuneId; any }>()
    const { project } = useProject()
    const projectId = project?.id || projectFromUri

    const summaryTableName = `project/${projectId}/ftspace/${spaceId}/eval/summary`
    const viewConfigName = `fine-tune-${spaceId}`

    return {
        summaryTableName,
        viewConfigName,
        defaultColumnKey: 'sys/id',
        projectId,
    }
}

export function useFineTuneInfo() {
    const { fineTune, setFineTune } = useFineTune()
    const { projectId, spaceId, fineTuneId } = useParams<{ projectId: any; spaceId: any; fineTuneId; any }>()
    const info = api.useFineTuneInfo(projectId, spaceId, fineTuneId)

    return {
        info,
        fineTune,
        setFineTune,
        params: {
            fineTuneId,
            projectId,
            spaceId,
        },
    }
}
