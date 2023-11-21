import React, { useCallback, useState } from 'react'
import Card from '@/components/Card'
import { createJob } from '@job/services/job'
import JobForm from '@job/components/JobForm'
import useTranslation from '@/hooks/useTranslation'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import { Prompt } from 'react-router-dom'
import _ from 'lodash'
import { ITableState, useEvaluationStore } from '@starwhale/ui/GridTable/store'
import { useFetchViewConfig } from '@/domain/evaluation/hooks/useFetchViewConfig'
import { setEvaluationViewConfig } from '@/domain/evaluation/services/evaluation'
import useFetchDatastoreByTable from '@starwhale/core/datastore/hooks/useFetchDatastoreByTable'
import { toaster } from 'baseui/toast'
import { BusyPlaceholder, ExtendButton } from '@starwhale/ui'
import { useLocalStorage } from 'react-use'
import { GridResizerVertical } from '@starwhale/ui/AutoResizer/GridResizerVertical'
import EvaluationListResult from './EvaluationListResult'
import GridCombineTable from '@starwhale/ui/GridTable/GridCombineTable'
import { val } from '@starwhale/ui/GridTable/utils'
import shallow from 'zustand/shallow'
import useDatastorePage from '@starwhale/core/datastore/hooks/useDatastorePage'
import { useEventCallback } from '@starwhale/core'
import { useDatastoreSummaryColumns } from '@starwhale/ui/GridDatastoreTable/hooks/useDatastoreSummaryColumns'
import { useFineTuneConfig } from '@/domain/space/hooks/useFineTune'

const selector = (s: ITableState) => ({
    rowSelectedIds: s.rowSelectedIds,
    currentView: s.currentView,
    initStore: s.initStore,
    getRawConfigs: s.getRawConfigs,
    onCurrentViewIdChange: s.onCurrentViewIdChange,
    getRawIfChangedConfigs: s.getRawIfChangedConfigs,
})

export default function FineTuneEvaluationListCard() {
    const config = useFineTuneConfig()

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return <EvaluationListCard {...config} />
}

export function EvaluationListCard({
    projectId,
    summaryTableName,
    defaultColumnKey,
    viewConfigName,
    viewCurrentKey,
    gotoTasks,
    gotoResults,
}) {
    const [t] = useTranslation()

    const { rowSelectedIds, currentView, initStore, onCurrentViewIdChange, getRawIfChangedConfigs } =
        useEvaluationStore(selector, shallow)
    const { page, setPage, params } = useDatastorePage({
        pageNum: 1,
        pageSize: 100,
        sortBy: currentView?.sortBy || defaultColumnKey,
        sortDirection: currentView?.sortBy ? (currentView?.sortDirection as any) : 'DESC',
        queries: currentView?.queries,
        tableName: summaryTableName,
    })
    const [changed, setChanged] = useState(false)
    const evaluationViewConfig = useFetchViewConfig(projectId, viewConfigName)
    const { columnTypes, records, columnHints } = useFetchDatastoreByTable(params, true)
    const [defaultViewObj, setDefaultViewObj] = useLocalStorage<Record<string, any>>(viewCurrentKey, {})
    const $columns = useDatastoreSummaryColumns({ projectId, columnTypes, columnHints, hasAction: true })

    const $ready = evaluationViewConfig.isSuccess

    React.useEffect(() => {
        const unloadCallback = (event: any) => {
            if (!changed) return ''
            event.preventDefault()
            // eslint-disable-next-line no-param-reassign
            event.returnValue = 'Some browsers display this to the user'
            return ''
        }

        window.addEventListener('beforeunload', unloadCallback)
        return () => {
            window.removeEventListener('beforeunload', unloadCallback)
        }
    }, [changed])

    const doSave = useEventCallback(() => {
        setEvaluationViewConfig(projectId, {
            name: viewConfigName,
            content: JSON.stringify(getRawIfChangedConfigs(), null),
        }).then(() => {
            toaster.positive(t('evaluation.save.success'), {})
        })
    })

    const onViewsChange = useEventCallback((state: ITableState, prevState: ITableState) => {
        setChanged(state.currentView.updated ?? false)
        setDefaultViewObj((obj) => {
            return { ...obj, [projectId]: state.currentView.id }
        })
        if (!_.isEqual(state.views, prevState.views)) {
            setEvaluationViewConfig(projectId, {
                name: viewConfigName,
                content: JSON.stringify(
                    {
                        ...getRawIfChangedConfigs(),
                        views: state.views,
                    },
                    null
                ),
            })
        }
    })

    const onCurrentViewChange = useEventCallback((state: ITableState) => {
        setDefaultViewObj((obj) => {
            return { ...obj, [projectId]: state.currentView.id }
        })
    })

    const initRef = React.useRef('')
    const viewId = defaultViewObj?.[projectId]
    React.useEffect(() => {
        if (!evaluationViewConfig.isSuccess) return
        if (initRef.current === projectId) return
        let $rawConfig
        try {
            $rawConfig = JSON.parse(evaluationViewConfig.data?.content, undefined)
        } catch (e) {
            $rawConfig = undefined
        }
        initStore($rawConfig)
        onCurrentViewIdChange(viewId)
        initRef.current = projectId
    }, [
        evaluationViewConfig.isSuccess,
        viewId,
        onCurrentViewIdChange,
        projectId,
        evaluationViewConfig.data?.content,
        initStore,
    ])

    const $compareRows = React.useMemo(() => {
        return records?.filter((r) => rowSelectedIds.includes(val(r.id))) ?? []
    }, [rowSelectedIds, records])

    if (!$ready) return <BusyPlaceholder />

    const getActions = (row: any) => [
        {
            access: true,
            quickAccess: true,
            component: ({ hasText }) => (
                <ExtendButton
                    isFull
                    icon='Detail'
                    tooltip={t('View Details')}
                    styleas={['menuoption', hasText ? undefined : 'highlight']}
                    onClick={() => gotoTasks(row)}
                >
                    {hasText ? t('View Details') : undefined}
                </ExtendButton>
            ),
        },
        {
            access: true,
            component: ({ hasText }) => (
                <ExtendButton
                    isFull
                    icon='tasks'
                    styleas={['menuoption', hasText ? undefined : 'highlight']}
                    onClick={() => gotoResults(row)}
                >
                    {hasText ? t('View Tasks') : undefined}
                </ExtendButton>
            ),
        },
    ]

    return (
        <>
            <Prompt when={changed} message='If you leave this page, your changes will be discarded.' />
            <GridResizerVertical
                top={() => (
                    <GridCombineTable
                        rowActions={getActions as any}
                        title={t('evaluation.title')}
                        titleOfCompare={t('compare.title')}
                        store={useEvaluationStore}
                        compareable
                        columnable
                        viewable
                        queryable
                        selectable
                        sortable
                        paginationable
                        page={page}
                        onPageChange={setPage}
                        records={records}
                        columnTypes={columnTypes}
                        columnHints={columnHints}
                        columns={$columns}
                        onSave={doSave}
                        onViewsChange={onViewsChange}
                        onCurrentViewChange={onCurrentViewChange}
                        emptyColumnMessage={
                            <BusyPlaceholder type='notfound'>{t('evalution.grid.empty.notice')}</BusyPlaceholder>
                        }
                    />
                )}
                isResizeable={$compareRows.length > 0}
                initGridMode={0}
                bottom={() => <EvaluationListResult rows={$compareRows} />}
                resizeTitle={t('evalution.result.title')}
            />
        </>
    )
}
