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
import { ITableState, useEvaluationDetailStore, useEvaluationCompareStore } from '@starwhale/ui/base/data-table/store'
import { useFetchViewConfig } from '@/domain/evaluation/hooks/useFetchViewConfig'
import { setEvaluationViewConfig } from '@/domain/evaluation/services/evaluation'
import { tableNameOfResult, tableNameOfSummary } from '@starwhale/core/datastore/utils'
import { TextLink } from '@/components/Link'
import { WithCurrentAuth } from '@/api/WithAuth'
import { GridTable } from '@starwhale/ui/GridTable'
import { useDatastoreColumns } from '@starwhale/ui/GridDatastoreTable'
import { toaster } from 'baseui/toast'
import EvaluationListCompare from './EvaluationListCompare'
import { BusyPlaceholder, Button, GridResizer } from '@starwhale/ui'
import { useLocalStorage } from 'react-use'
import { useProject } from '@project/hooks/useProject'
import JobStatus from '@/domain/job/components/JobStatus'
import useFetchDatastoreByTables from '@starwhale/core/datastore/hooks/useFetchDatastoreByTables'
import ToolBar from '@starwhale/ui/GridTable/components/ToolBar'

export default function DatastoreDiffTables() {
    const { expandedWidth, expanded } = useDrawer()
    const [t] = useTranslation()
    const history = useHistory()
    const { projectId: projectFromUri } = useParams<{ projectId: string }>()
    const { project } = useProject()
    const projectId = project?.id || projectFromUri

    const store = useEvaluationDetailStore()

    const queries = React.useMemo(
        () =>
            new Array(10).fill(0).map((_, i) => {
                return {
                    tableName: tableNameOfResult(projectId, 'cec3345ffd2a4748bac26def597f04a2'),
                    columnPrefix: `result-${i}-`,
                    // ,
                }
            }),
        [projectId]
    )
    const getId = useCallback((record) => {
        return record['result-0-id']?.value || record['result-0-id']
    }, [])

    const { records, columnTypes, columnInfo } = useFetchDatastoreByTables({
        tables: queries,
    })
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
        return records.filter((r) => store.rowSelectedIds.includes(getId(r))) ?? []
    }, [store.rowSelectedIds, records, getId])

    const $ready = React.useMemo(() => {
        return columnInfo.isSuccess && evaluationViewConfig.isSuccess
    }, [columnInfo.isSuccess, evaluationViewConfig.isSuccess])

    console.log(store.rowSelectedIds, $compareRows, store)

    const doSave = useCallback(() => {
        setEvaluationViewConfig(projectId, {
            name: 'evaluation',
            content: JSON.stringify(store.getRawConfigs(), null),
        }).then(() => {
            toaster.positive(t('evaluation.save.success'), {})
        })
        return {}
    }, [projectId, store.getRawConfigs, t])

    const doChange = React.useCallback(
        async (state: ITableState, prevState: ITableState) => {
            if (!$ready) return
            setChanged(state.currentView.updated ?? false)
            setViewId(state.currentView.id)

            if (!_.isEqual(state.views, prevState.views)) {
                // auto save views
                // eslint-disable-next-line no-console
                console.log('saved views', state.views, prevState.views)
                setEvaluationViewConfig(projectId, {
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
        },
        [$ready, projectId, setViewId, store.getRawConfigs, t]
    )

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
                            store={useEvaluationDetailStore}
                            columnable
                            // viewable
                            // queryable
                            selectable
                            columns={$columnsWithSpecColumns}
                            getId={getId}
                            data={records}
                            onSave={doSave as any}
                            onChange={doChange}
                            emptyColumnMessage={
                                <BusyPlaceholder type='notfound'>
                                    Create a new evaluation or Config to add columns
                                </BusyPlaceholder>
                            }
                        ></GridTable>
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
                                getId={getId}
                            />
                        </Card>
                    )
                }}
            />
        </Card>
    )
}
