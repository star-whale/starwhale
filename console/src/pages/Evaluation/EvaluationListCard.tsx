import React, { useCallback, useState } from 'react'
import Card from '@/components/Card'
import { createJob } from '@job/services/job'
import { ICreateJobSchema } from '@job/schemas/job'
import JobForm from '@job/components/JobForm'
import useTranslation from '@/hooks/useTranslation'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import { useHistory, useParams, Prompt } from 'react-router-dom'
import _ from 'lodash'
import { ITableState, useEvaluationStore } from '@starwhale/ui/GridTable/store'
import { useFetchViewConfig } from '@/domain/evaluation/hooks/useFetchViewConfig'
import { setEvaluationViewConfig } from '@/domain/evaluation/services/evaluation'
import useFetchDatastoreByTable from '@starwhale/core/datastore/hooks/useFetchDatastoreByTable'
import { tableNameOfSummary } from '@starwhale/core/datastore/utils'
import { WithCurrentAuth } from '@/api/WithAuth'
import { toaster } from 'baseui/toast'
import { BusyPlaceholder, Button } from '@starwhale/ui'
import { useLocalStorage } from 'react-use'
import { useProject } from '@project/hooks/useProject'
import { GridResizerVertical } from '@starwhale/ui/AutoResizer/GridResizerVertical'
import EvaluationListResult from './EvaluationListResult'
import GridCombineTable from '@starwhale/ui/GridTable/GridCombineTable'
import { val } from '@starwhale/ui/GridTable/utils'
import shallow from 'zustand/shallow'
import useDatastorePage from '@starwhale/core/datastore/hooks/useDatastorePage'
import { useEventCallback } from '@starwhale/core'
import { useDatastoreSummaryColumns } from '@starwhale/ui/GridDatastoreTable/hooks/useDatastoreSummaryColumns'

const selector = (s: ITableState) => ({
    rowSelectedIds: s.rowSelectedIds,
    currentView: s.currentView,
    initStore: s.initStore,
    getRawConfigs: s.getRawConfigs,
    onCurrentViewIdChange: s.onCurrentViewIdChange,
    getRawIfChangedConfigs: s.getRawIfChangedConfigs,
})

export default function EvaluationListCard() {
    const [t] = useTranslation()
    const history = useHistory()
    const { projectId: projectFromUri } = useParams<{ projectId: string }>()
    const { project } = useProject()
    const projectId = project?.id || projectFromUri
    const summaryTableName = React.useMemo(() => {
        return tableNameOfSummary(projectId)
    }, [projectId])
    const { rowSelectedIds, currentView, initStore, onCurrentViewIdChange, getRawIfChangedConfigs } =
        useEvaluationStore(selector, shallow)

    const { page, setPage, params } = useDatastorePage({
        pageNum: 1,
        pageSize: 100,
        sortBy: currentView?.sortBy || 'sys/id',
        sortDirection: currentView?.sortBy ? (currentView?.sortDirection as any) : 'DESC',
        queries: currentView?.queries,
        tableName: summaryTableName,
    })

    const { recordInfo: evaluationsInfo, columnTypes, records, columnHints } = useFetchDatastoreByTable(params, true)
    const evaluationViewConfig = useFetchViewConfig(projectId, 'evaluation')
    const [isCreateJobOpen, setIsCreateJobOpen] = useState(false)
    const [defaultViewObj, setDefaultViewObj] = useLocalStorage<Record<string, any>>('currentViewId', {})
    const [changed, setChanged] = useState(false)
    const handleCreateJob = useCallback(
        async (data: ICreateJobSchema) => {
            await createJob(projectId, data)
            await evaluationsInfo.refetch()
            setIsCreateJobOpen(false)
        },
        [evaluationsInfo, projectId]
    )

    const $columns = useDatastoreSummaryColumns({ projectId, columnTypes, columnHints })

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
            name: 'evaluation',
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
                name: 'evaluation',
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

    if (!$ready)
        return (
            <Card
                title={t('Evaluations')}
                style={{
                    flexShrink: 1,
                    marginBottom: 0,
                    width: '100%',
                    flex: 1,
                }}
                extra={
                    <WithCurrentAuth id='evaluation.create'>
                        <Button onClick={() => history.push('new_job')}>{t('create')}</Button>
                    </WithCurrentAuth>
                }
            >
                <BusyPlaceholder />
            </Card>
        )
    return (
        <Card
            title={t('Evaluations')}
            style={{
                flexShrink: 1,
                marginBottom: 0,
                width: '100%',
                flex: 1,
            }}
            bodyStyle={{
                flexDirection: 'column',
            }}
            extra={
                <WithCurrentAuth id='evaluation.create'>
                    <Button onClick={() => history.push('new_job')}>{t('create')}</Button>
                </WithCurrentAuth>
            }
        >
            <Prompt when={changed} message='If you leave this page, your changes will be discarded.' />
            <GridResizerVertical
                top={() => (
                    <GridCombineTable
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
            <Modal isOpen={isCreateJobOpen} onClose={() => setIsCreateJobOpen(false)} closeable animate autoFocus>
                <ModalHeader>{t('create sth', [t('Job')])}</ModalHeader>
                <ModalBody>
                    <JobForm onSubmit={handleCreateJob} />
                </ModalBody>
            </Modal>
        </Card>
    )
}
