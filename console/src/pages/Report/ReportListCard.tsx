import React from 'react'
import { useFetchReports } from '@/domain/report/hooks/useReport'
import { useHistory, useParams } from 'react-router-dom'
import Card from '@/components/Card'
import Table from '@/components/Table'
import useTranslation from '@/hooks/useTranslation'
import { IReportSchema } from '@/domain/report/schemas/report'
import { TextLink } from '@/components/Link'
import { Button, IconFont, QueryInput, Toggle, useConfirmCtx } from '@starwhale/ui'
import { toaster } from 'baseui/toast'
import { removeReport, updateReportShared } from '@/domain/report/services/report'
import Text from '@starwhale/ui/Text'
import Copy from 'react-copy-to-clipboard'
import { usePage } from '@/hooks/usePage'
import { formatTimestampDateTime } from '@/utils/datetime'
import { WithCurrentAuth } from '@/api/WithAuth'
import { Tooltip } from '@starwhale/ui/Tooltip'
import Link from '@/components/Link/Link'

export default function ReportListCard() {
    const [t] = useTranslation()
    const [page] = usePage()
    const { projectId } = useParams<{ projectId: string }>()
    const [filter, setFilter] = React.useState('')
    const reports = useFetchReports(projectId, { ...page, search: filter })
    const confirmCtx = useConfirmCtx()
    const history = useHistory()

    const handleDelete = async (id: number, title: string) => {
        const ok = await confirmCtx.show({ title: t('Confirm'), content: t('delete sth confirm', [title]) })
        if (!ok) {
            return
        }
        await removeReport(projectId, id)
        await reports.refetch()
    }

    const renderRow = (report: IReportSchema) => {
        const Share = (
            <Toggle
                key='shared'
                value={report.shared}
                onChange={async (share) => {
                    try {
                        await updateReportShared(projectId, report.id, share)
                        await reports.refetch()
                        toaster.positive(t('dataset.overview.shared.success'))
                    } catch (e) {
                        toaster.negative(t('dataset.overview.shared.fail'))
                    }
                }}
            />
        )
        return [
            <TextLink key='title' to={`/projects/${projectId}/reports/${report.id}`} style={{ maxWidth: '300px' }}>
                {report.title}
            </TextLink>,
            !report.shared ? (
                <Copy
                    key='shared'
                    text={`${window.location.origin}/simple/report/preview/?rid=${report.uuid}`}
                    onCopy={() => {
                        toaster.positive(t('Copied'), { autoHideDuration: 1000 })
                    }}
                >
                    <div>{Share}</div>
                </Copy>
            ) : (
                Share
            ),
            <Text key='desc' style={{ maxWidth: '300px' }} tooltip={report.description}>
                {report.description}
            </Text>,
            report.owner.name,
            report.createdTime ? formatTimestampDateTime(report.createdTime) : '',
            report.modifiedTime ? formatTimestampDateTime(report.modifiedTime) : '',
            <div key='action' style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                {report.shared && (
                    <>
                        <Tooltip content={t('Preview')} showArrow placement='top'>
                            <div>
                                <Link target='_blank' to={`/simple/report/preview/?rid=${report.uuid}`}>
                                    <IconFont type='link' />
                                </Link>
                            </div>
                        </Tooltip>
                        <Tooltip content={t('Copy Link')} showArrow placement='top'>
                            <div>
                                <Copy
                                    text={`${window.location.origin}/simple/report/preview/?rid=${report.uuid}`}
                                    onCopy={() => {
                                        toaster.positive(t('Copied'), { autoHideDuration: 1000 })
                                    }}
                                >
                                    <Button as='link' icon='a-copylink' onClick={() => {}} />
                                </Copy>
                            </div>
                        </Tooltip>
                    </>
                )}
                <Tooltip content={t('Delete')} showArrow placement='top'>
                    <div>
                        <Button as='link' icon='delete' onClick={() => handleDelete(report.id, report.title)} />
                    </div>
                </Tooltip>
            </div>,
        ]
    }

    return (
        <Card
            title={t('Reports')}
            extra={
                <WithCurrentAuth id='report.create'>
                    <Button onClick={() => history.push('reports/new')}>{t('create')}</Button>
                </WithCurrentAuth>
            }
        >
            <div style={{ maxWidth: '280px', paddingBottom: '10px' }}>
                <QueryInput
                    placeholder={t('report.search.by.title')}
                    onChange={(val: string) => {
                        setFilter(val)
                    }}
                />
            </div>
            <Table
                isLoading={reports.isLoading}
                columns={[
                    t('report.title'),
                    t('Shared'),
                    t('Description'),
                    t('Owner'),
                    t('Created'),
                    t('Update'),
                    t('Action'),
                ]}
                data={reports.data?.list.map(renderRow)}
                paginationProps={{
                    start: reports.data?.pageNum,
                    count: reports.data?.pageSize,
                    total: reports.data?.total,
                    afterPageChange: async () => {
                        await reports.refetch()
                    },
                }}
            />
        </Card>
    )
}
