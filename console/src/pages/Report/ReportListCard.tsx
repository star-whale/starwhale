import React from 'react'
import { useFetchReports } from '@/domain/report/hooks/useReport'
import { useHistory, useParams } from 'react-router-dom'
import Card from '@/components/Card'
import Table from '@/components/Table'
import useTranslation from '@/hooks/useTranslation'
import { IReportSchema } from '@/domain/report/schemas/report'
import { Button, ExtendButton, QueryInput, Toggle, useConfirmCtx } from '@starwhale/ui'
import { toaster } from 'baseui/toast'
import { removeReport, updateReportShared } from '@/domain/report/services/report'
import Text from '@starwhale/ui/Text'
import Copy from 'react-copy-to-clipboard'
import { usePage } from '@/hooks/usePage'
import { formatTimestampDateTime } from '@/utils/datetime'
import { WithCurrentAuth, useAccess } from '@/api/WithAuth'
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

    const isAccessReportDelete = useAccess('report.delete')
    const getActions = (report: IReportSchema) =>
        [
            {
                access: true,
                quickAccess: true,
                component: ({ hasText }) => (
                    <ExtendButton
                        isFull
                        icon='overview'
                        styleas={['menuoption', hasText ? undefined : 'highlight']}
                        tooltip={!hasText ? t('View Details') : undefined}
                        onClick={() => history.push(`/projects/${projectId}/reports/${report.id}`)}
                    >
                        {hasText ? t('View Details') : undefined}
                    </ExtendButton>
                ),
            },
            {
                access: report.shared,
                component: ({ hasText }) => (
                    <Link
                        target='_blank'
                        to={`/simple/report/preview/?rid=${report.uuid}`}
                        style={{ color: 'rgb(43, 101, 217)' }}
                    >
                        <ExtendButton icon='link' styleas={['menuoption', hasText ? undefined : 'highlight']}>
                            {hasText ? t('Preview') : undefined}
                        </ExtendButton>
                    </Link>
                ),
            },
            {
                access: report.shared,
                component: ({ hasText }) => (
                    <div style={{ lineHeight: 1 }}>
                        <Copy
                            text={`${window.location.origin}/simple/report/preview/?rid=${report.uuid}`}
                            onCopy={() => {
                                toaster.positive(t('Copied'), { autoHideDuration: 1000 })
                            }}
                        >
                            <ExtendButton icon='a-copylink' styleas={['menuoption', hasText ? undefined : 'highlight']}>
                                {hasText ? t('Copy Link') : undefined}
                            </ExtendButton>
                        </Copy>
                    </div>
                ),
            },
            {
                access: isAccessReportDelete,
                component: ({ hasText }) => (
                    <ExtendButton
                        icon='delete'
                        styleas={['menuoption', hasText ? undefined : 'highlight', 'negative']}
                        onClick={() => handleDelete(report.id, report.title)}
                    >
                        {hasText ? t('Delete') : undefined}
                    </ExtendButton>
                ),
            },
        ].filter(Boolean)

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
            <div key='title' style={{ maxWidth: '300px' }}>
                {report.title}
            </div>,
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
                renderActions={(rowIndex) => {
                    const data = reports.data?.list[rowIndex]
                    if (!data) return undefined
                    return getActions(data)
                }}
                isLoading={reports.isLoading}
                columns={[t('report.title'), t('Shared'), t('Description'), t('Owner'), t('Created'), t('Update')]}
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
