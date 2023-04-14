import React, { useCallback, useMemo, useState } from 'react'
import Card from '@/components/Card'
import { createJob } from '@job/services/job'
import { ICreateJobSchema } from '@job/schemas/job'
import JobForm from '@job/components/JobForm'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import { useHistory, useParams, Prompt } from 'react-router-dom'
import { CustomColumn, StringColumn } from '@starwhale/ui/base/data-table'
import { useDrawer } from '@/hooks/useDrawer'
import _ from 'lodash'
import {
    ITableState,
    useEvaluationCompareStore,
    useEvaluationDetailStore,
    useEvaluationStore,
} from '@starwhale/ui/base/data-table/store'
import { useFetchViewConfig } from '@/domain/evaluation/hooks/useFetchViewConfig'
import { setEvaluationViewConfig } from '@/domain/evaluation/services/evaluation'
import { tableNameOfResult, tableNameOfSummary } from '@starwhale/core/datastore/utils'
import { TextLink } from '@/components/Link'
import { WithCurrentAuth } from '@/api/WithAuth'
import { GridTable, useDatastoreColumns } from '@starwhale/ui/GridTable'
import { toaster } from 'baseui/toast'
import EvaluationListCompare from './EvaluationListCompare'
import { BusyPlaceholder, Button, GridResizer } from '@starwhale/ui'
import { useLocalStorage } from 'react-use'
import { useProject } from '@project/hooks/useProject'
import JobStatus from '@/domain/job/components/JobStatus'
import { useDatastore, useFetchDatastoreAllTables } from '@starwhale/core/datastore'
import useFetchDatastoreByTables from '@starwhale/core/datastore/hooks/useFetchDatastoreByTables'

export default function EvaluationListResult() {
    const { expandedWidth, expanded } = useDrawer()
    const [t] = useTranslation()
    const history = useHistory()
    const { projectId: projectFromUri } = useParams<{ projectId: string }>()
    const { project } = useProject()
    const projectId = project?.id || projectFromUri

    const store = useEvaluationDetailStore()

    const options = React.useMemo(() => {
        const sorts = store.currentView?.sortBy
            ? [
                  {
                      columnName: store.currentView?.sortBy,
                      descending: store.currentView?.sortDirection === 'DESC',
                  },
              ]
            : []

        return {
            pageNum: 1,
            pageSize: 1000,
            query: {
                orderBy: sorts,
            },
            filter: store.currentView.queries,
        }
    }, [store.currentView.queries, store.currentView.sortBy, store.currentView.sortDirection])

    const queries = React.useMemo(
        () =>
            new Array(10).fill(0).map((_, i) => {
                return {
                    tableName: tableNameOfResult(projectId, '9607791f26894b6eb1d117b7c50721f3'),
                    prefix: `result-${i}-`,
                    options,
                }
            }),
        [projectId, options]
    )

    const { records, columnTypes } = useFetchDatastoreByTables(queries)
    const evaluationViewConfig = useFetchViewConfig(projectId, 'evaluation-detail')
    const [viewId, setViewId] = useLocalStorage<string>('currentViewId', '')
    const [changed, setChanged] = useState(false)
    const $columns = useDatastoreColumns(columnTypes)

    const $columnsWithSpecColumns = useMemo(() => {
        return $columns.map((column) => {
            return {
                ...column,
                fillWidth: false,
            }
        })
    }, [t, $columns, projectId])

    const $compareRows = React.useMemo(() => {
        return records.filter((r) => store.rowSelectedIds.includes(r.id)) ?? []
    }, [store.rowSelectedIds, records])

    // const $ready = React.useMemo(() => {
    //     return columnInfo.isSuccess && evaluationViewConfig.isSuccess
    // }, [columnInfo.isSuccess, evaluationViewConfig.isSuccess])
    const $ready = true

    console.log(store.rowSelectedIds, $compareRows)

    React.useEffect(() => {
        const unloadCallback = (event: any) => {
            if (!changed) return ''
            event.preventDefault()
            // eslint-disable-next-line no-param-reassign
            event.returnValue = 'Confirm Save Changes'
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
        // store.initStore($rawConfig)
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
        >
            <Prompt when={changed} message='If you leave this page, your changes will be discarded.' />
            <GridResizer
                left={() => {
                    return (
                        <GridTable
                            store={useEvaluationStore}
                            columnable
                            // viewable
                            // queryable
                            selectable
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
        </Card>
    )
}
