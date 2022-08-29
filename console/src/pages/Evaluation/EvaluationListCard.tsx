import React, { useCallback, useMemo, useState } from 'react'
import Card from '@/components/Card'
import { createJob } from '@job/services/job'
import { ICreateJobSchema } from '@job/schemas/job'
import JobForm from '@job/components/JobForm'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import { Button, SIZE as ButtonSize } from 'baseui/button'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import Table from '@/components/Table/TableTyped'
import { Link, useHistory, useParams } from 'react-router-dom'
import IconFont from '@/components/IconFont'
import { CustomColumn, StringColumn } from '@/components/data-table'
import { useDrawer } from '@/hooks/useDrawer'
import { useFetchEvaluations } from '@/domain/evaluation/hooks/useFetchEvaluations'
import { useFetchEvaluationAttrs } from '@/domain/evaluation/hooks/useFetchEvaluationAttrs'
import { usePage } from '@/hooks/usePage'
import { ColumnT } from '@/components/data-table/types'
import { IEvaluationAttributeValue } from '@/domain/evaluation/schemas/evaluation'
import _ from 'lodash'
import { useStyletron } from 'baseui'
import { ITableState, useEvaluationCompareStore, useEvaluationStore } from '@/components/data-table/store'
import { useFetchViewConfig } from '@/domain/evaluation/hooks/useFetchViewConfig'
import { setEvaluationViewConfig } from '@/domain/evaluation/services/evaluation'
import { useQueryDatasetList } from '@/domain/datastore/hooks/useFetchDatastore'
import { tableNameOfSummary } from '@/domain/datastore/utils'
import EvaluationListCompare from './EvaluationListCompare'
import { useProject } from '@/domain/project/hooks/useProject'
import { useFetchProject } from '@/domain/project/hooks/useFetchProject'

const gridLayout = [
    // RIGHT:
    '0px 10px 1fr',
    // MIDDLE:
    '1fr 10px 1fr',
    // LEFT:
    '1fr 10px 0px',
]

export default function EvaluationListCard() {
    const [css] = useStyletron()
    const { expandedWidth, expanded } = useDrawer()
    const [t] = useTranslation()
    const history = useHistory()
    const [page] = usePage()
    const { projectId } = useParams<{ projectId: string }>()
    const evaluationsInfo = useFetchEvaluations(projectId, { pageNum: 1, pageSize: 1000 })
    const evaluationAttrsInfo = useFetchEvaluationAttrs(projectId, page)
    const evaluationViewConfig = useFetchViewConfig(projectId, 'evaluation')
    const { project } = useProject()

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

    const summaryTableName = React.useMemo(() => {
        return tableNameOfSummary(project?.name as string)
    }, [projectId])
    const summaryTable = useQueryDatasetList(summaryTableName, { pageNum: 0, pageSize: 1000 })
    console.log(summaryTable.data)

    // TODO
    // 1. column key should be equal with eva attr field
    // 2. prefix of summary/ can be manageable for now
    const columns: ColumnT[] = useMemo(
        () => [
            CustomColumn({
                key: 'uuid',
                title: t('Evaluation ID'),
                // filterable: true,
                // renderFilter: () => <div>1</div>,
                mapDataToValue: (item: any) => item,
                // @ts-ignore
                renderCell: (props: any) => {
                    const item = props.value

                    return (
                        <Link key={item.id} to={`/projects/${projectId}/evaluations/${item.id}/results`}>
                            {`${item.modelName}-${item.id}`}
                        </Link>
                    )
                },
            }),
            StringColumn({
                key: 'modelName',
                title: t('sth name', [t('Model')]),
                filterable: true,
                mapDataToValue: (data: any) => data.modelName,
            }),
            StringColumn({
                key: 'modelVersion',
                title: t('Model Version'),
                mapDataToValue: (data: any) => data.modelVersion,
            }),
            StringColumn({
                key: 'owner',
                title: t('Owner'),
                mapDataToValue: (item: any) => item.owner,
            }),
            CustomColumn({
                key: 'duration',
                title: t('Runtime'),
                sortable: true,
                filterType: 'number',
                sortFn: (a: any, b: any) => {
                    // eslint-disable-next-line
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
            }),
            StringColumn({
                key: 'createTime',
                title: t('Created'),
                mapDataToValue: (data: any) => data.createdTime && formatTimestampDateTime(data.createdTime),
            }),
            StringColumn({
                key: 'stopTime',
                title: t('End Time'),
                mapDataToValue: (data: any) => (data.stopTime > 0 ? formatTimestampDateTime(data.stopTime) : '-'),
            }),
        ],
        [projectId, t]
    )
    const $columnsWithAttrs = useMemo(() => {
        const columnsWithAttrs = [...columns]

        evaluationAttrsInfo?.data?.forEach((attr) => {
            if (!attr.name.startsWith('summary/')) {
                return
            }

            const name = attr.name.split('/').slice(1).join('/')

            switch (attr.type) {
                default:
                case 'string':
                    columnsWithAttrs.push(
                        StringColumn({
                            key: attr.name,
                            title: name,
                            filterType: 'string',
                            mapDataToValue: (data: any) => data.attributes?.[attr.name],
                        })
                    )
                    break
                case 'float':
                case 'int':
                    columnsWithAttrs.push(
                        CustomColumn({
                            key: attr.name,
                            title: name,
                            sortable: true,
                            filterType: 'number',
                            sortFn: (a: any, b: any) => {
                                // eslint-disable-next-line
                                const aNum = Number(a)
                                const bNum = Number(b)
                                if (Number.isNaN(aNum)) {
                                    return -1
                                }
                                return aNum - bNum
                            },
                            // @ts-ignore
                            renderCell: (props: any) => {
                                return <p title={props?.value}>{props?.value}</p>
                            },
                            mapDataToValue: (data: any): string =>
                                data.attributes?.find((v: IEvaluationAttributeValue) => v.name === attr.name)?.value ??
                                '-',
                        })
                    )
                    break
            }
        })

        return columnsWithAttrs
    }, [evaluationAttrsInfo, columns])

    const [compareRows, setCompareRows] = useState<any[]>([])

    const batchAction = useMemo(
        () => [
            {
                label: 'Compare',
                onClick: ({ selection }: any) => {
                    const rows = selection.map((item: any) => item.data)
                    setCompareRows(rows)
                },
            },
        ],
        [setCompareRows]
    )

    const $data = useMemo(
        () =>
            evaluationsInfo.data?.list?.map((raw) => {
                const $attributes = raw.attributes?.filter((item: any) => _.startsWith(item.name, 'summary'))
                return {
                    ...raw,
                    attributes: $attributes,
                }
            }) ?? [],
        [evaluationsInfo.data]
    )

    const [gridMode, setGridMode] = useState(1)
    const resizeRef = React.useRef<HTMLDivElement>(null)
    const gridRef = React.useRef<HTMLDivElement>(null)
    const leftRef = React.useRef<HTMLDivElement | null>(null)

    const grdiModeRef = React.useRef(1)
    const resize = useCallback(
        (e: MouseEvent) => {
            window.requestAnimationFrame(() => {
                if (resizeRef.current && leftRef.current) {
                    const offset = resizeRef.current.getBoundingClientRect().left - e.clientX
                    // leftRef.current!.style.width = `${leftRef.current?.getBoundingClientRect().width - offset}px`
                    // leftRef.current!.style.flexBasis = `${leftRef.current?.getBoundingClientRect().width - offset}px`
                    // console.log('resize', leftRef.current?.getBoundingClientRect(), e.clientX, offset)
                    const newWidth = leftRef.current?.getBoundingClientRect().width - offset
                    // eslint-disable-next-line
                    if (newWidth + 300 > gridRef.current!.getBoundingClientRect().width) {
                        grdiModeRef.current = 2
                        setGridMode(2)
                    } else if (newWidth < 440) {
                        grdiModeRef.current = 0
                        setGridMode(0)
                    } else if (grdiModeRef.current === 1) {
                        // eslint-disable-next-line
                        gridRef.current!.style.gridTemplateColumns = `${Math.max(
                            newWidth,
                            440
                        )}px 10px minmax(400px, 1fr)`
                    }
                }
            })
        },
        [grdiModeRef, setGridMode]
    )
    const resizeEnd = () => {
        document.body.style.userSelect = 'unset'
        document.body.style.cursor = 'unset'
        document.removeEventListener('mouseup', resizeEnd)
        document.removeEventListener('mousemove', resize)
    }
    const resizeStart = () => {
        if (gridMode !== 1) return
        grdiModeRef.current = 1
        document.body.style.userSelect = 'none'
        document.body.style.cursor = 'col-resize'
        document.addEventListener('mouseup', resizeEnd)
        document.addEventListener('mousemove', resize)
    }
    const handleResizeStart = resizeStart

    React.useEffect(() => {
        return resizeEnd
    })

    const handleResize = useCallback(
        (dir) => {
            let next = Math.min(gridLayout.length - 1, gridMode + dir)
            next = Math.max(0, next)
            grdiModeRef.current = next
            setGridMode(next)
        },
        [gridMode, setGridMode, grdiModeRef]
    )

    React.useEffect(() => {
        setCompareRows($data.filter((r) => store.rowSelectedIds.includes(r.id)))
    }, [store.rowSelectedIds, $data])

    React.useEffect(() => {
        const unsub = useEvaluationCompareStore.subscribe(
            (state: ITableState) => state.rowSelectedIds,
            (state: any[]) => store.onSelectMany(state)
        )
        return unsub
    }, [store, $data])

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
    }, [store, $data, projectId, evaluationViewConfig])

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
        <div
            ref={gridRef}
            style={{
                display: 'grid',
                gridTemplateColumns: compareRows.length === 0 ? '1fr' : gridLayout[gridMode],
                overflow: 'hidden',
                width: '100%',
                flex: 1,
            }}
        >
            <Card
                onMountCard={(ref) => {
                    leftRef.current = ref
                }}
                title={t('Evaluations')}
                style={{
                    marginRight: expanded ? expandedWidth : '0',
                    flexShrink: 1,
                    minWidth: '440px',
                    marginBottom: 0,
                }}
                extra={
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
                }
            >
                <Table
                    useStore={useEvaluationStore}
                    searchable
                    filterable
                    columnable
                    viewable
                    batchActions={batchAction}
                    isLoading={evaluationsInfo.isLoading}
                    columns={$columnsWithAttrs}
                    data={$data}
                />
                <Modal isOpen={isCreateJobOpen} onClose={() => setIsCreateJobOpen(false)} closeable animate autoFocus>
                    <ModalHeader>{t('create sth', [t('Job')])}</ModalHeader>
                    <ModalBody>
                        <JobForm onSubmit={handleCreateJob} />
                    </ModalBody>
                </Modal>
            </Card>
            {compareRows.length > 0 && (
                <>
                    {/* eslint-disable-next-line jsx-a11y/role-has-required-aria-props */}
                    <div
                        ref={resizeRef}
                        className={css({
                            'width': '10px',
                            'flexBasis': '10px',
                            'cursor': 'col-resize',
                            'paddingTop': '112px',
                            'zIndex': 20,
                            'overflow': 'visible',
                            ':hover': {
                                backgroundColor: '',
                            },
                            'position': 'relative',
                            'right': gridMode === 2 ? '14px' : undefined,
                            'left': gridMode === 0 ? '4px' : undefined,
                        })}
                        role='button'
                        tabIndex={0}
                        onMouseDown={handleResizeStart}
                    >
                        <i
                            role='button'
                            tabIndex={0}
                            className='resize-left resize-left--hover'
                            onClick={() => handleResize(1)}
                        >
                            <IconFont
                                type='fold2'
                                size={12}
                                style={{
                                    color: gridMode !== 2 ? undefined : '#ccc',
                                    transform: 'rotate(-90deg) translateY(-2px)',
                                    marginBottom: '2px',
                                }}
                            />
                        </i>
                        <i
                            role='button'
                            tabIndex={0}
                            className='resize-right resize-right--hover'
                            onClick={() => handleResize(-1)}
                        >
                            <IconFont
                                type='unfold2'
                                size={12}
                                style={{
                                    color: gridMode !== 0 ? undefined : '#ccc',
                                    transform: 'rotate(-90deg) translateY(2px)',
                                }}
                            />
                        </i>
                    </div>
                    <Card
                        title={t('Compare Evaluations')}
                        style={{ marginRight: expanded ? expandedWidth : '0', marginBottom: 0 }}
                    >
                        <EvaluationListCompare rows={compareRows} attrs={evaluationAttrsInfo?.data ?? []} />
                    </Card>
                </>
            )}
        </div>
    )
}
