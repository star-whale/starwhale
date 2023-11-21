import React from 'react'
import { api } from '@/api'
import useGlobalState from '@/hooks/global'
import { useHistory, useParams } from 'react-router-dom'
import { useEventCallback } from '@starwhale/core'
import { useProject } from '@/domain/project/hooks/useProject'
import { val } from '@starwhale/ui/GridTable/utils'

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

export const useFineTuneConfig = () => {
    const {
        projectId: projectFromUri,
        spaceId,
        fineTuneId,
    } = useParams<{ projectId: any; spaceId: any; fineTuneId; any }>()
    const { project } = useProject()
    const history = useHistory()

    const projectId = project?.id || projectFromUri
    const summaryTableName = `project/${projectId}/ftspace/${spaceId}/eval/summary`
    const viewConfigName = `fine-tune-${spaceId}`
    const viewCurrentKey = 'fine-tune-view-id'
    const defaultColumnKey = 'sys/id'

    const gotoTasks = useEventCallback((row) => {
        history.push(`/projects/${projectId}/spaces/${spaceId}/${val(row?.data?.[defaultColumnKey])}/results`)
    })
    const gotoResults = useEventCallback((row) => {
        history.push(`/projects/${projectId}/spaces/${spaceId}/${val(row?.data?.[defaultColumnKey])}/results`)
    })

    return {
        summaryTableName,
        viewConfigName,
        viewCurrentKey,
        defaultColumnKey,
        projectId,
        gotoTasks,
        gotoResults,
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
