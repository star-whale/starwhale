import React, { useCallback, useMemo, useState } from 'react'
import Card from '@/components/Card'
import { createJob } from '@job/services/job'
import { ICreateJobSchema } from '@job/schemas/job'
import JobForm from '@job/components/JobForm'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import { Button, SIZE as ButtonSize } from 'baseui/button'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import { useHistory, useParams } from 'react-router-dom'
import IconFont from '@/components/IconFont'
import { CustomColumn, StringColumn } from '@starwhale/ui/base/data-table'
import { useDrawer } from '@/hooks/useDrawer'
import _ from 'lodash'
import { ITableState, useEvaluationCompareStore, useEvaluationStore } from '@starwhale/ui/base/data-table/store'
import { useFetchViewConfig } from '@/domain/evaluation/hooks/useFetchViewConfig'
import { setEvaluationViewConfig } from '@/domain/evaluation/services/evaluation'
import { useQueryDatasetList } from '@starwhale/core/datastore/hooks/useFetchDatastore'
import { tableNameOfSummary } from '@starwhale/core/datastore/utils'
import { useProject } from '@/domain/project/hooks/useProject'
import { TextLink } from '@/components/Link'
import { WithCurrentAuth } from '@/api/WithAuth'
import GridResizer from '@/components/AutoResizer/GridResizer'
import { GridTable, useDatastoreColumns } from '@starwhale/ui/GridTable'
import EvaluationListCompare from './EvaluationListCompare'

const page = { pageNum: 1, pageSize: 1000 }

export default function EvaluationListCard() {
    const { expandedWidth, expanded } = useDrawer()
    const [t] = useTranslation()
    const history = useHistory()
    const { projectId } = useParams<{ projectId: string }>()
    const { project } = useProject()
    const summaryTableName = React.useMemo(() => {
        if (!project?.name) return ''
        return tableNameOfSummary(project?.name as string)
    }, [project])
    const evaluationsInfo = useQueryDatasetList(summaryTableName, page, true)
    const evaluationViewConfig = useFetchViewConfig(projectId, 'evaluation')

    const [isCreateJobOpen, setIsCreateJobOpen] = useState(false)
    const handleCreateJob = useCallback(
        async (data: ICreateJobSchema) => {
            await createJob(projectId, data)
            await evaluationsInfo.refetch()
            setIsCreateJobOpen(false)
        },
        [evaluationsInfo, projectId]
    )

    const store = useEvaluationStore()

    const $columns = useDatastoreColumns(
        evaluationsInfo?.data?.columnTypes?.sort((ca, cb) => {
            if (ca.name === 'id') return -1
            if (ca.name?.startsWith('sys/')) return -1
            if (ca.name > cb.name) return -1
            return 1
        }) ?? []
    )

    const $columnsWithSpecColumns = useMemo(() => {
        return $columns.map((column) => {
            if (column.key === 'id')
                return CustomColumn({
                    key: column.key,
                    title: column.key,
                    mapDataToValue: (item: any) => item.id,
                    // @ts-ignore
                    renderCell: (props: any) => {
                        const { data } = props ?? {}
                        if (!data) return <></>

                        return (
                            <TextLink key={data.id} to={`/projects/${projectId}/evaluations/${data.id}/results`}>
                                {`${data.id}`}
                            </TextLink>
                        )
                    },
                })
            if (column.key === 'sys/duration')
                return CustomColumn({
                    key: 'duration',
                    title: t('Elapsed Time'),
                    sortable: true,
                    filterType: 'number',
                    sortFn: (a: any, b: any) => {
                        const aNum = Number(a)
                        const bNum = Number(b)
                        if (Number.isNaN(aNum)) {
                            return -1
                        }
                        return aNum - bNum
                    },
                    // @ts-ignore
                    renderCell: (props: any) => {
                        return <p title={props?.value}>{durationToStr(props?.value)}</p>
                    },
                    mapDataToValue: (data: any): string => data.duration,
                })
            if (column.key?.endsWith('time'))
                return StringColumn({
                    key: column.key,
                    title: column.key,
                    mapDataToValue: (data: any) =>
                        column.key && data[column.key] && formatTimestampDateTime(data[column.key]),
                })

            return column
        })
    }, [t, $columns, projectId])

    const [compareRows, setCompareRows] = useState<any[]>([])

    React.useEffect(() => {
        if (evaluationsInfo.isSuccess)
            setCompareRows(evaluationsInfo.data?.records?.filter((r) => store.rowSelectedIds.includes(r.id)) ?? [])
    }, [store.rowSelectedIds, evaluationsInfo.isSuccess])

    console.log(evaluationsInfo)

    React.useEffect(() => {
        const unsub = useEvaluationCompareStore.subscribe(
            (state: ITableState) => state.rowSelectedIds,
            (state: any[]) => store.onSelectMany(state)
        )
        return unsub
    }, [store])

    // sync local to api
    React.useEffect(() => {
        const unsub = useEvaluationStore.subscribe(
            (state: ITableState) => state,
            async (state: ITableState, prevState: ITableState) => {
                if (
                    !_.isEqual(store.getRawConfigs(state), store.getRawConfigs(prevState)) &&
                    evaluationViewConfig.isSuccess
                ) {
                    // console.log('changed state', store.getRawConfigs(state), store.getRawConfigs(prevState))
                    await setEvaluationViewConfig(projectId, {
                        name: 'evaluation',
                        content: JSON.stringify(store.getRawConfigs()),
                    })
                }
            }
        )
        return unsub
    }, [store, projectId, evaluationViewConfig])

    // sync api to local
    React.useEffect(() => {
        if (evaluationViewConfig.isSuccess && evaluationViewConfig.data) {
            let apiState
            try {
                apiState = JSON.parse(evaluationViewConfig.data?.content, undefined)
                if (!_.isEqual(apiState, store.getRawConfigs())) {
                    // console.log('upcoming state', apiState, evaluationViewConfig.data)
                    store.setRawConfigs(apiState)
                }
            } catch (e) {
                // console.log(e)
            }
        }
        // store should not be used as a deps, it's will trigger cycle render
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [evaluationViewConfig.isSuccess, evaluationViewConfig.data])

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
                    <Button
                        startEnhancer={<IconFont type='add' kind='white' />}
                        size={ButtonSize.compact}
                        onClick={() => {
                            history.push('new_job')
                        }}
                        isLoading={evaluationsInfo.isLoading}
                    >
                        {t('create')}
                    </Button>
                </WithCurrentAuth>
            }
        >
            <GridResizer
                left={() => {
                    return (
                        <GridTable
                            store={useEvaluationStore}
                            columnable
                            viewable
                            queryable
                            selectable
                            isLoading={evaluationsInfo.isLoading}
                            columns={$columnsWithSpecColumns}
                            data={evaluationsInfo.data?.records ?? []}
                        />
                    )
                }}
                isResizeable={compareRows.length > 0}
                right={() => {
                    return (
                        <Card style={{ marginRight: expanded ? expandedWidth : '0', marginBottom: 0 }}>
                            <EvaluationListCompare
                                title={t('Compare Evaluations')}
                                rows={compareRows}
                                attrs={evaluationsInfo?.data?.columnTypes}
                            />
                        </Card>
                    )
                }}
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
