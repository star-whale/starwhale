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
import EvaluationListCompare from './EvaluationListCompare'

export default function EvaluationListCard() {
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
                mapDataToValue: (data: any) => data.modelName,
            }),
            StringColumn({
                key: 'modelVersion',
                title: t('Version'),
                mapDataToValue: (data: any) => data.modelVersion,
            }),
            StringColumn({
                key: 'owner',
                title: t('Owner'),
                mapDataToValue: (item: any) => item.owner,
            }),
            StringColumn({
                key: 'duration',
                title: t('Run time'),
                mapDataToValue: (data: any) => (typeof data.duration === 'string' ? '-' : durationToStr(data.duration)),
            }),
            StringColumn({
                key: 'createTime',
                title: t('Created time'),
                mapDataToValue: (data: any) => data.createdTime && formatTimestampDateTime(data.createdTime),
            }),
            StringColumn({
                key: 'stopTime',
                title: t('End time'),
                mapDataToValue: (data: any) => (data.stopTime > 0 ? formatTimestampDateTime(data.stopTime) : '-'),
            }),
            // CustomColumn({
            //     key: 'action',
            //     title: t('Action'),
            //     // @ts-ignore
            //     renderCell: (props: any) => {
            //         const data = props.value ?? {}
            //         const actions: Partial<Record<JobStatusType, React.ReactNode>> = {
            //             [JobStatusType.CREATED]: (
            //                 <>
            //                     <StyledLink onClick={() => handleAction(data.id, JobActionType.CANCEL)}>
            //                         {t('Cancel')}
            //                     </StyledLink>
            //                     &nbsp;&nbsp;
            //                     <StyledLink onClick={() => handleAction(data.id, JobActionType.PAUSE)}>
            //                         {t('Pause')}
            //                     </StyledLink>
            //                 </>
            //             ),
            //             [JobStatusType.RUNNING]: (
            //                 <>
            //                     <StyledLink onClick={() => handleAction(data.id, JobActionType.CANCEL)}>
            //                         {t('Cancel')}
            //                     </StyledLink>
            //                     &nbsp;&nbsp;
            //                     <StyledLink onClick={() => handleAction(data.id, JobActionType.PAUSE)}>
            //                         {t('Pause')}
            //                     </StyledLink>
            //                 </>
            //             ),
            //             [JobStatusType.PAUSED]: (
            //                 <>
            //                     <StyledLink onClick={() => handleAction(data.id, JobActionType.CANCEL)}>
            //                         {t('Cancel')}
            //                     </StyledLink>
            //                     &nbsp;&nbsp;
            //                     <StyledLink onClick={() => handleAction(data.id, JobActionType.RESUME)}>
            //                         {t('Resume')}
            //                     </StyledLink>
            //                 </>
            //             ),
            //             [JobStatusType.SUCCESS]: (
            //                 <Link to={`/projects/${projectId}/evaluations/${data.id}/results`}>
            //                     {t('View Results')}
            //                 </Link>
            //             ),
            //         }
            //         return actions[data.jobStatus as JobStatusType] ?? ''
            //     },
            //     mapDataToValue: (item: any) => item,
            // }),
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

    const [compareRows, setCompareRows] = useState([])
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
        []
    )

    return (
        <>
            <Card
                title={t('Evaluations')}
                style={{ marginRight: expanded ? expandedWidth : '0' }}
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
                    id='evaluations'
                    batchActions={batchAction}
                    isLoading={evaluationsInfo.isLoading}
                    columns={$columnsWithAttrs}
                    // @ts-ignore
                    data={evaluationsInfo.data?.list ?? []}
                />
                <Modal isOpen={isCreateJobOpen} onClose={() => setIsCreateJobOpen(false)} closeable animate autoFocus>
                    <ModalHeader>{t('create sth', [t('Job')])}</ModalHeader>
                    <ModalBody>
                        <JobForm onSubmit={handleCreateJob} />
                    </ModalBody>
                </Modal>
            </Card>
            {compareRows.length > 0 && (
                <Card title={t('Compare Evaluations')} style={{ marginRight: expanded ? expandedWidth : '0' }}>
                    <EvaluationListCompare rows={compareRows} attrs={evaluationAttrsInfo?.data ?? []} />
                </Card>
            )}
        </>
    )
}
