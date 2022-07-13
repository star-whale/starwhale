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
import EvaluationListCompare from './EvaluationListCompare'
import ResizeLeft from '@/assets/left.svg'
import ResizeLeftShadow from '@/assets/left-shadow.png'
import ResizeRight from '@/assets/right.svg'
import ResizeRightShadow from '@/assets/right-shadow.png'
import { useStyletron } from 'baseui'
import { relative } from 'path'
import { RowT } from 'baseui/data-table'

export default function EvaluationListCard() {
    const [css] = useStyletron()
    const { expandedWidth, expanded } = useDrawer()
    const [t] = useTranslation()
    const history = useHistory()
    const [page] = usePage()
    const { projectId } = useParams<{ projectId: string }>()
    const evaluationsInfo = useFetchEvaluations(projectId, { pageNum: 1, pageSize: 1000 })
    const evaluationAttrsInfo = useFetchEvaluationAttrs(projectId, page)

    const [isCreateJobOpen, setIsCreateJobOpen] = useState(false)
    const handleCreateJob = useCallback(
        async (data: ICreateJobSchema) => {
            await createJob(projectId, data)
            await evaluationsInfo.refetch()
            setIsCreateJobOpen(false)
        },
        [evaluationsInfo, projectId]
    )
    // const handleAction = useCallback(
    //     async (jobId, type: JobActionType) => {
    //         await doJobAction(projectId, jobId, type)
    //         toaster.positive(t('job action done'), { autoHideDuration: 2000 })
    //         await evaluationsInfo.refetch()
    //         setIsCreateJobOpen(false)
    //     },
    //     [evaluationsInfo, projectId, t]
    // )

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
            StringColumn({
                key: 'duration',
                title: t('Runtime'),
                mapDataToValue: (data: any) => (typeof data.duration === 'string' ? '-' : durationToStr(data.duration)),
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
                                return <p title={props?.value}>{props?.value.slice(0, 6)}</p>
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

    const [compareRows, setCompareRows] = useState<RowT[]>([])
    const handleSelectChange = useCallback((selection: RowT[]) => {
        const rows = selection.map((item: any) => item.data)
        setCompareRows(rows)
    }, [])
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

    const gridLayout = [
        // RIGHT:
        '0px 10px 1fr',
        // MIDDLE:
        '1fr 10px 1fr',
        // LEFT:
        '1fr 10px 0px',
    ]
    const [gridMode, setGridMode] = useState(1)
    const resizeRef = React.useRef<HTMLDivElement>(null)
    const gridRef = React.useRef<HTMLDivElement>(null)
    const leftRef = React.useRef<HTMLDivElement | null>(null)
    const [resizeWidth, setResizeWidth] = useState(0)
    const resizeEnd = () => {
        document.body.style.userSelect = 'unset'
        document.body.style.cursor = 'unset'
        document.removeEventListener('mouseup', resizeEnd)
        document.removeEventListener('mousemove', resize)
    }
    const grdiModeRef = React.useRef(1)
    const resizeStart = () => {
        if (gridMode != 1) return
        grdiModeRef.current == 1
        document.body.style.userSelect = 'none'
        document.body.style.cursor = 'col-resize'
        document.addEventListener('mouseup', resizeEnd)
        document.addEventListener('mousemove', resize)
    }
    const resize = useCallback(
        (e: MouseEvent) => {
            console.log(resizeWidth, gridMode, grdiModeRef.current)

            window.requestAnimationFrame(() => {
                if (resizeRef.current && leftRef.current) {
                    const offset = resizeRef.current.getBoundingClientRect().left - e.clientX
                    // leftRef.current!.style.width = `${leftRef.current?.getBoundingClientRect().width - offset}px`
                    // leftRef.current!.style.flexBasis = `${leftRef.current?.getBoundingClientRect().width - offset}px`
                    // console.log('resize', leftRef.current?.getBoundingClientRect(), e.clientX, offset)
                    const newWidth = leftRef.current?.getBoundingClientRect().width - offset
                    if (newWidth + 300 > gridRef.current!.getBoundingClientRect().width) {
                        grdiModeRef.current = 2
                        setGridMode(2)
                    } else if (newWidth < 440) {
                        grdiModeRef.current = 0
                        setGridMode(0)
                    } else if (grdiModeRef.current == 1) {
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

    return (
        <div
            ref={gridRef}
            style={{
                display: 'grid',
                gridTemplateColumns: compareRows.length === 0 ? '1fr' : gridLayout[gridMode],
                overflow: 'hidden',
                width: '100%',
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
                    // gridColumn:
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
                    // @ts-ignore
                    // onColumnSave={(columnSortedIds, columnVisibleIds, sortedIds) => {
                    //     console.log(columnSortedIds, columnVisibleIds)
                    // }}
                    searchable
                    filterable
                    columnable
                    viewable
                    id='evaluations'
                    batchActions={batchAction}
                    isLoading={evaluationsInfo.isLoading}
                    columns={$columnsWithAttrs}
                    onSelectionChange={handleSelectChange}
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
                                backgroundColor: '#E',
                            },
                            'position': 'relative',
                            'right': gridMode == 2 ? '14px' : undefined,
                            'left': gridMode == 0 ? '4px' : undefined,
                        })}
                        onMouseDown={handleResizeStart}
                    >
                        <i className='resize-left resize-left--hover' onClick={() => handleResize(1)}>
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
                        <i className='resize-right resize-right--hover' onClick={() => handleResize(-1)}>
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
