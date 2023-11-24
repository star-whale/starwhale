import React from 'react'
import { api } from '@/api'
import { useHistory, useParams } from 'react-router-dom'
import { useEventCallback } from '@starwhale/core'
import { useProject } from '@/domain/project/hooks/useProject'
import { val } from '@starwhale/ui/GridTable/utils'
import { ExtendButton } from '@starwhale/ui/Button'
import useTranslation from '@/hooks/useTranslation'
import { useEvaluationStore } from '@starwhale/ui/GridTable/store'
import { toaster } from 'baseui/toast'

export const useFineTuneEvaluation = () => {
    const {
        projectId: projectFromUri,
        spaceId,
        jobId,
    } = useParams<{ projectId: any; spaceId: any; fineTuneId; any; jobId: any }>()
    const [t] = useTranslation()
    const { project } = useProject()
    const history = useHistory()

    const projectId = project?.id || projectFromUri
    const summaryTableName = `project/${projectId}/ftspace/${spaceId}/eval/summary`
    const projectSummaryTableName = `project/${projectId}/eval/summary`
    const viewConfigKey = `fine-tune-${spaceId}`
    const viewCurrentKey = 'fine-tune-view-id'
    const defaultColumnKey = 'sys/id'

    const routes = {
        evaluations: `/projects/${projectId}/spaces/${spaceId}/fine-tune-evals`,
        evaluationDetail: `/projects/${projectId}/spaces/${spaceId}/fine-tune-evals/${jobId}`,
        evaluationOverview: `/projects/${projectId}/spaces/${spaceId}/evaluations/${jobId}/overview`,
    }
    const gotoList = useEventCallback(() => {
        history.push(`/projects/${projectId}/spaces/${spaceId}/fine-tune-evals`)
    })
    const gotoDetails = useEventCallback((row) => {
        history.push(`/projects/${projectId}/spaces/${spaceId}/fine-tune-evals/${val(row?.data?.[defaultColumnKey])}`)
    })
    const gotoTasks = useEventCallback((row) => {
        history.push(`/projects/${projectId}/spaces/${spaceId}/evaluations/${val(row?.data?.[defaultColumnKey])}/tasks`)
    })
    const gotoResults = useEventCallback((row) => {
        history.push(
            `/projects/${projectId}/spaces/${spaceId}/evaluations/${val(row?.data?.[defaultColumnKey])}/results`
        )
    })

    const summaryTableQuery = React.useMemo(() => {
        if (!summaryTableName || !jobId) return undefined

        return {
            tableName: summaryTableName,
            start: 0,
            limit: 1,
            rawResult: true,
            ignoreNonExistingTable: true,
            filter: {
                operator: 'EQUAL',
                operands: [
                    {
                        intValue: jobId,
                    },
                    {
                        columnName: 'sys/id',
                    },
                ],
            },
        }
    }, [jobId, summaryTableName])

    // const summaryTableExportQuery = React.useMemo(() => {
    //     if (!summaryTableName || !jobId) return undefined

    //     return {
    //         tableName: summaryTableName,
    //         start: 0,
    //         limit: 1,
    //         rawResult: true,
    //         ignoreNonExistingTable: true,
    //         filter: {
    //             operator: 'EQUAL',
    //             operands: [
    //                 {
    //                     intValue: jobId,
    //                 },
    //                 {
    //                     columnName: 'sys/id',
    //                 },
    //             ],
    //         },
    //     }
    // }, [jobId, summaryTableName])

    const getActions = (row) => {
        return [
            {
                access: true,
                quickAccess: true,
                component: ({ hasText }) => (
                    <ExtendButton
                        isFull
                        icon='Detail'
                        tooltip={t('View Details')}
                        styleas={['menuoption', hasText ? undefined : 'highlight']}
                        onClick={() => gotoDetails(row)}
                    >
                        {hasText ? t('View Details') : undefined}
                    </ExtendButton>
                ),
            },
            {
                access: true,
                quickAccess: true,
                component: ({ hasText }) => (
                    <ExtendButton
                        isFull
                        icon='tasks'
                        tooltip={!hasText ? t('View Tasks') : undefined}
                        styleas={['menuoption', hasText ? undefined : 'highlight']}
                        onClick={() => gotoTasks(row)}
                    >
                        {hasText ? t('View Tasks') : undefined}
                    </ExtendButton>
                ),
            },
        ]
    }

    const toastProps = {
        autoHideDuration: 2000,
        overrides: { Body: { style: { minWidth: '500px' } } },
    }

    const importEval = useEventCallback(async (ids) => {
        const { success, fail } = await api.importEval(projectId, spaceId, {
            ids,
        })
        if (fail && fail > 0) {
            toaster.negative(t('ft.job.model.import.fail', [success, fail]), toastProps)
        } else if (success === 0) {
            toaster.warning(t('ft.job.model.import.warning'), toastProps)
        } else {
            toaster.positive(t('ft.job.model.import.done'), toastProps)
        }
    })

    const exportEval = useEventCallback(async (ids) => {
        const { success, fail } = await api.exportEval(projectId, spaceId, {
            ids,
        })
        if (fail && fail > 0) {
            toaster.negative(t('ft.job.model.release.fail', [success, fail]), toastProps)
        } else if (success === 0) {
            toaster.warning(t('ft.job.model.release.warning'), toastProps)
        } else {
            toaster.positive(t('ft.job.model.release.done'), toastProps)
        }
    })

    return {
        projectSummaryTableName,
        summaryTableName,
        summaryTableQuery,
        viewConfigKey,
        viewCurrentKey,
        defaultColumnKey,
        projectId,
        spaceId,
        jobId,
        gotoTasks,
        gotoResults,
        gotoDetails,
        gotoList,
        getActions,
        exportEval,
        importEval,
        routes,
        useStore: useEvaluationStore,
    }
}

export default useFineTuneEvaluation
