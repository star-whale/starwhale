import React, { useCallback, useMemo, useState } from 'react'
import Card from '@/components/Card'
import { createJob } from '@job/services/job'
import { ICreateJobSchema } from '@job/schemas/job'
import JobForm from '@job/components/JobForm'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import { useHistory, useParams, Prompt } from 'react-router-dom'
import { CustomColumn } from '@starwhale/ui/base/data-table'
import { useDrawer } from '@/hooks/useDrawer'
import _ from 'lodash'
import { ITableState, useEvaluationCompareStore, useEvaluationStore } from '@starwhale/ui/base/data-table/store'
import { useFetchViewConfig } from '@/domain/evaluation/hooks/useFetchViewConfig'
import { setEvaluationViewConfig } from '@/domain/evaluation/services/evaluation'
import { useQueryDatasetList } from '@starwhale/core/datastore/hooks/useFetchDatastore'
import { tableNameOfSummary } from '@starwhale/core/datastore/utils'
import { TextLink } from '@/components/Link'
import { WithCurrentAuth } from '@/api/WithAuth'
import { GridTable, useDatastoreColumns } from '@starwhale/ui/GridTable'
import { toaster } from 'baseui/toast'
import EvaluationListCompare from './EvaluationListCompare'
import { BusyPlaceholder, Button, GridResizer } from '@starwhale/ui'
import { useLocalStorage } from 'react-use'
import { useProject } from '@project/hooks/useProject'
import JobStatus from '@/domain/job/components/JobStatus'
import { useDatastore } from '@starwhale/core/datastore'
import { createUseStyles } from 'react-jss'

const useStyles = createUseStyles({
    showDetail: {
        background: '#fff',
        // boxShadow: '0px 2px 8px 0  rgba(0, 0, 0, 0.12)',
        borderTop: '1px solid #e5e5e5',
        height: '44px',
        display: 'flex',
        alignItems: 'flex-end',
        justifyContent: 'center',
    },
})

export default function EvaluationListCard() {
    const styles = useStyles()
    const { expandedWidth, expanded } = useDrawer()
    const [t] = useTranslation()
    const history = useHistory()
    const { projectId: projectFromUri } = useParams<{ projectId: string }>()
    const { project } = useProject()
    const projectId = project?.id || projectFromUri
    const summaryTableName = React.useMemo(() => {
        return tableNameOfSummary(projectId)
    }, [projectId])
    const store = useEvaluationStore()

    const options = React.useMemo(() => {
        const sorts = store.currentView?.sortBy
            ? [
                  {
                      columnName: store.currentView?.sortBy,
                      descending: store.currentView?.sortDirection === 'DESC',
                  },
              ]
            : []

        sorts.push({
            columnName: 'sys/id',
            descending: true,
        })

        return {
            pageNum: 1,
            pageSize: 1000,
            query: {
                orderBy: sorts,
            },
            filter: store.currentView.queries,
        }
    }, [store.currentView.queries, store.currentView.sortBy, store.currentView.sortDirection])

    const {
        columnInfo,
        recordInfo: evaluationsInfo,
        columnTypes,
    } = useQueryDatasetList(summaryTableName, options, true)
    const evaluationViewConfig = useFetchViewConfig(projectId, 'evaluation')
    const [isCreateJobOpen, setIsCreateJobOpen] = useState(false)
    const [viewId, setViewId] = useLocalStorage<string>('currentViewId', '')
    const [changed, setChanged] = useState(false)
    const handleCreateJob = useCallback(
        async (data: ICreateJobSchema) => {
            await createJob(projectId, data)
            await evaluationsInfo.refetch()
            setIsCreateJobOpen(false)
        },
        [evaluationsInfo, projectId]
    )

    const { records } = useDatastore(evaluationsInfo?.data?.records)
    const $columns = useDatastoreColumns(columnTypes)

    const $columnsWithSpecColumns = useMemo(() => {
        return $columns.map((column) => {
            if (column.key === 'sys/id')
                return CustomColumn({
                    ...column,
                    key: column.key,
                    title: column.key,
                    fillWidth: false,
                    mapDataToValue: (data: any) => _.get(data, [column.key, 'value'], 0),
                    // @ts-ignore
                    renderCell: ({ value: id }) => {
                        if (!id) return <></>
                        return (
                            <TextLink key={id} to={`/projects/${projectId}/evaluations/${id}/results`}>
                                {id}
                            </TextLink>
                        )
                    },
                })
            if (column.key === 'sys/duration')
                return CustomColumn({
                    ...column,
                    key: 'duration',
                    title: t('Elapsed Time'),
                    sortable: true,
                    filterType: 'number',
                    fillWidth: false,
                    sortFn: (a: any, b: any) => {
                        const aNum = Number(a)
                        const bNum = Number(b)
                        if (Number.isNaN(aNum)) {
                            return -1
                        }
                        return aNum - bNum
                    },
                    // @ts-ignore
                    renderCell: ({ value }) => <p title={value}>{durationToStr(value)}</p>,
                    mapDataToValue: (data: any): number => _.get(data, [column.key, 'value'], 0),
                })
            if (column.key === 'sys/job_status')
                return CustomColumn({
                    ...column,
                    key: column.key,
                    title: column.key,
                    sortable: true,
                    fillWidth: false,
                    // @ts-ignore
                    renderCell: ({ value }) => (
                        <div title={value}>
                            <JobStatus status={value} />
                        </div>
                    ),
                    mapDataToValue: (data: any): string => _.get(data, [column.key, 'value'], ''),
                })
            if (column.key?.endsWith('time')) {
                return CustomColumn({
                    ...column,
                    key: column.key,
                    title: column.key,
                    fillWidth: false,
                    // @ts-ignore
                    renderCell: ({ value }) => {
                        return <p title={value}>{formatTimestampDateTime(value)}</p>
                    },
                    mapDataToValue: (data: any) => _.get(data, [column.key, 'value'], 0),
                })
            }
            return {
                ...column,
                fillWidth: false,
            }
        })
    }, [t, $columns, projectId])

    const $compareRows = React.useMemo(() => {
        return records.filter((r) => store.rowSelectedIds.includes(r.id)) ?? []
    }, [store.rowSelectedIds, records])

    const $ready = React.useMemo(() => {
        return columnInfo.isSuccess && evaluationViewConfig.isSuccess
    }, [columnInfo.isSuccess, evaluationViewConfig.isSuccess])

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

    const doSave = async () => {
        await setEvaluationViewConfig(projectId, {
            name: 'evaluation',
            content: JSON.stringify(store.getRawConfigs(), null),
        })
        toaster.positive(t('evaluation.save.success'), {})
        return {}
    }

    const doChange = async (state: ITableState, prevState: ITableState) => {
        if (!$ready) return
        setChanged(state.currentView.updated ?? false)
        setViewId(state.currentView.id)

        if (!_.isEqual(state.views, prevState.views)) {
            // auto save views
            // eslint-disable-next-line no-console
            console.log('saved views', state.views, prevState.views)
            await setEvaluationViewConfig(projectId, {
                name: 'evaluation',
                content: JSON.stringify(
                    {
                        ...store.getRawConfigs(),
                        views: state.views,
                    },
                    null
                ),
            })
        }
    }

    // NOTICE: use isinit to make sure view config is loading into store
    const initRef = React.useRef(false)
    React.useEffect(() => {
        if (!evaluationViewConfig.isSuccess) return
        if (initRef.current) return

        let $rawConfig
        try {
            $rawConfig = JSON.parse(evaluationViewConfig.data?.content, undefined) ?? {}
        } catch (e) {
            // console.log(e)
        }
        // eslint-disable-next-line no-console
        console.log('init store')
        store.initStore($rawConfig)
        store.onCurrentViewIdChange(viewId)

        initRef.current = true
        // store should not be used as a deps, it's will trigger cycle render
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [evaluationViewConfig.isSuccess, evaluationViewConfig.data?.content, viewId])

    React.useEffect(() => {
        const unsub = useEvaluationCompareStore.subscribe(
            (state: ITableState) => state.rowSelectedIds,
            (state: any[]) => store.onSelectMany(state)
        )
        return unsub
    }, [store])

    if (!$ready)
        return (
            <Card
                title={t('Evaluations')}
                style={{
                    marginRight: expanded ? expandedWidth : '0',
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
                marginRight: expanded ? expandedWidth : '0',
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
            <Prompt when={changed} message='If you leave this page, your changes will be discarded.' />
            <GridResizer
                left={() => {
                    return (
                        <GridTable
                            store={useEvaluationStore}
                            columnable
                            viewable
                            queryable
                            selectable
                            isLoading={evaluationsInfo.isLoading || evaluationViewConfig.isLoading}
                            columns={$columnsWithSpecColumns}
                            data={records}
                            onSave={doSave as any}
                            onChange={doChange}
                            emptyColumnMessage={
                                <BusyPlaceholder type='notfound'>
                                    Create a new evaluation or Config to add columns
                                </BusyPlaceholder>
                            }
                        />
                    )
                }}
                isResizeable={$compareRows.length > 0}
                right={() => {
                    return (
                        <Card style={{ marginRight: expanded ? expandedWidth : '0', marginBottom: 0 }}>
                            <EvaluationListCompare
                                title={t('Compare Evaluations')}
                                rows={$compareRows}
                                attrs={columnTypes}
                            />
                        </Card>
                    )
                }}
            />
            <div className={styles.showDetail}>
                <Button
                    kind='tertiary'
                    onClick={() => {}}
                    icon='unfold2'
                    overrides={{
                        BaseButton: {
                            style: {
                                paddingTop: '9px',
                                paddingBottom: '9px',
                            },
                        },
                    }}
                >
                    {t('compare.show.details')}
                </Button>
            </div>
            <Modal isOpen={isCreateJobOpen} onClose={() => setIsCreateJobOpen(false)} closeable animate autoFocus>
                <ModalHeader>{t('create sth', [t('Job')])}</ModalHeader>
                <ModalBody>
                    <JobForm onSubmit={handleCreateJob} />
                </ModalBody>
            </Modal>
        </Card>
    )
}
