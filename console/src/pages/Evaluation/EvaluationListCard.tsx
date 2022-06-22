import React, { useCallback, useState } from 'react'
import Card from '@/components/Card'
import { createJob, doJobAction } from '@job/services/job'
import { usePage } from '@/hooks/usePage'
import { ICreateJobSchema, JobActionType, JobStatusType } from '@job/schemas/job'
import JobForm from '@job/components/JobForm'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import { Button, SIZE as ButtonSize } from 'baseui/button'
import User from '@/domain/user/components/User'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import Table from '@/components/Table/TableTyped'
import { Link, useHistory, useParams } from 'react-router-dom'
import { useFetchJobs } from '@job/hooks/useFetchJobs'
import { StyledLink } from 'baseui/link'
import { toaster } from 'baseui/toast'
import IconFont from '@/components/IconFont'
import { CustomColumn, CategoricalColumn, StringColumn } from '@/components/data-table'

export default function EvaluationListCard() {
    const [t] = useTranslation()
    const history = useHistory()
    const [page] = usePage()
    const { projectId } = useParams<{ projectId: string }>()
    const evaluationsInfo = useFetchJobs(projectId, page)
    const [isCreateJobOpen, setIsCreateJobOpen] = useState(false)
    const handleCreateJob = useCallback(
        async (data: ICreateJobSchema) => {
            await createJob(projectId, data)
            await evaluationsInfo.refetch()
            setIsCreateJobOpen(false)
        },
        [evaluationsInfo, projectId]
    )
    const handleAction = useCallback(
        async (jobId, type: JobActionType) => {
            await doJobAction(projectId, jobId, type)
            toaster.positive(t('job action done'), { autoHideDuration: 2000 })
            await evaluationsInfo.refetch()
            setIsCreateJobOpen(false)
        },
        [evaluationsInfo, projectId, t]
    )

    const columns = [
        StringColumn({
            key: 'evaluationId',
            title: t('Evaluation ID'),
            // renderCell: (props: any) => {
            //     const row = props.value ?? {}
            //     return (
            //         <Link
            //             style={{ alignSelf: 'center' }}
            //             key={row.id}
            //             to={`/projects/${projectId}/evaluations/${row.id}/actions`}
            //         >
            //             {row.uuid}
            //         </Link>
            //     )
            // },
            mapDataToValue: (item: any) => item.uuid,
        }),
        StringColumn({
            key: 'model',
            title: t('sth name', [t('Model')]),
            mapDataToValue: (data: any) => data.modelName,
        }),
        StringColumn({
            key: 'version',
            title: t('Version'),
            mapDataToValue: (data: any) => data.modelVersion,
        }),
        CustomColumn({
            key: 'owner',
            title: t('Owner'),
            // @ts-ignore
            renderCell: (props: any) => {
                const row = props.value ?? {}
                return <User user={row.owner} />
            },
            mapDataToValue: (item: any) => item,
        }),
        StringColumn({
            key: 'createtime',
            title: t('Created time'),
            mapDataToValue: (data: any) => data.createdTime && formatTimestampDateTime(data.createdTime),
        }),
        StringColumn({
            key: 'runtime',
            title: t('Run time'),
            mapDataToValue: (data: any) => (typeof data.duration === 'string' ? '-' : durationToStr(data.duration)),
        }),
        StringColumn({
            key: 'endtime',
            title: t('End time'),
            mapDataToValue: (data: any) => (data.stopTime > 0 ? formatTimestampDateTime(data.stopTime) : '-'),
        }),
        CategoricalColumn({
            key: 'status',
            title: t('Status'),
            mapDataToValue: (data: any) => data.jobStatus,
        }),
        CustomColumn({
            key: 'action',
            title: t('Action'),
            // @ts-ignore
            renderCell: (props: any) => {
                const data = props.value ?? {}
                const actions: Partial<Record<JobStatusType, React.ReactNode>> = {
                    [JobStatusType.CREATED]: (
                        <>
                            <StyledLink onClick={() => handleAction(data.id, JobActionType.CANCEL)}>
                                {t('Cancel')}
                            </StyledLink>
                            &nbsp;&nbsp;
                            <StyledLink onClick={() => handleAction(data.id, JobActionType.PAUSE)}>
                                {t('Pause')}
                            </StyledLink>
                        </>
                    ),
                    [JobStatusType.RUNNING]: (
                        <>
                            <StyledLink onClick={() => handleAction(data.id, JobActionType.CANCEL)}>
                                {t('Cancel')}
                            </StyledLink>
                            &nbsp;&nbsp;
                            <StyledLink onClick={() => handleAction(data.id, JobActionType.PAUSE)}>
                                {t('Pause')}
                            </StyledLink>
                        </>
                    ),
                    [JobStatusType.PAUSED]: (
                        <>
                            <StyledLink onClick={() => handleAction(data.id, JobActionType.CANCEL)}>
                                {t('Cancel')}
                            </StyledLink>
                            &nbsp;&nbsp;
                            <StyledLink onClick={() => handleAction(data.id, JobActionType.RESUME)}>
                                {t('Resume')}
                            </StyledLink>
                        </>
                    ),
                    [JobStatusType.SUCCESS]: (
                        <Link to={`/projects/${projectId}/evaluations/${data.id}/results`}>{t('View Results')}</Link>
                    ),
                }
                return actions[data.jobStatus as JobStatusType] ?? ''
            },
            mapDataToValue: (item: any) => item,
        }),
    ]

    // const [$columns, setColumns] = useState([] as any)

    // useEffect(() => {
    //     const id = setInterval(() => {
    //         setColumns(_.shuffle(columns))
    //     }, 2000)
    //     return () => {
    //         clearInterval(id)
    //     }
    // }, [])

    return (
        <>
            <Card
                title={t('Evaluations')}
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
                    isLoading={evaluationsInfo.isLoading}
                    columns={columns}
                    // @ts-ignore
                    data={evaluationsInfo.data?.list ?? []}
                    paginationProps={{
                        start: evaluationsInfo.data?.pageNum,
                        count: evaluationsInfo.data?.pageSize,
                        total: evaluationsInfo.data?.total,
                        afterPageChange: () => {
                            evaluationsInfo.refetch()
                        },
                    }}
                />
                <Modal isOpen={isCreateJobOpen} onClose={() => setIsCreateJobOpen(false)} closeable animate autoFocus>
                    <ModalHeader>{t('create sth', [t('Job')])}</ModalHeader>
                    <ModalBody>
                        <JobForm onSubmit={handleCreateJob} />
                    </ModalBody>
                </Modal>
            </Card>
        </>
    )
}
