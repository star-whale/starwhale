import React from 'react'
import { useFetchReports } from '@/domain/report/hooks/useReport'
import { useParams } from 'react-router-dom'
import Card from '@/components/Card'
import Table from '@/components/Table'
import useTranslation from '@/hooks/useTranslation'
import { IReportSchema } from '@/domain/report/schemas/report'
import { TextLink } from '@/components/Link'
import { Button, QueryInput, Toggle, useConfirmCtx } from '@starwhale/ui'
import { toaster } from 'baseui/toast'
import { updateReportShared } from '@/domain/report/services/report'
import Text from '@starwhale/ui/Text'
import Copy from 'react-copy-to-clipboard'

export default function ReportListCard() {
    const [t] = useTranslation()
    const { projectId } = useParams<{ projectId: string }>()
    const [filter, setFilter] = React.useState('')
    const reports = useFetchReports(projectId, filter)
    const confirmCtx = useConfirmCtx()

    const handleDelete = async (id: number, title: string) => {
        const ok = await confirmCtx.show({ title: t('Confirm'), content: t('delete sth confirm', [title]) })
        if (ok) {
            // eslint-disable-next-line no-console
            console.log('delete', id)
        }
    }

    const renderRow = (report: IReportSchema) => {
        return [
            <TextLink key='title' to={`/projects/${projectId}/reports/${report.id}`}>
                {report.title}
            </TextLink>,
            <Toggle
                key='shared'
                value={report.shared === 1}
                onChange={async (share) => {
                    try {
                        await updateReportShared(projectId, report.id, share)
                        toaster.positive(t('dataset.overview.shared.success'))
                    } catch (e) {
                        toaster.negative(t('dataset.overview.shared.fail'))
                    }
                }}
            />,
            <Text key='desc'>{report.description}</Text>,
            report.owner,
            report.createdTime,
            report.updatedTime,
            <div key='action' style={{ display: 'flex', gap: '5px' }}>
                {/* TODO: get link from the server */}
                <Copy
                    text={`${window.location.origin}/projects/${projectId}/reports/${report.id}`}
                    onCopy={() => {
                        toaster.positive(t('Copied'), { autoHideDuration: 1000 })
                    }}
                >
                    <Button as='link' icon='a-copylink' onClick={() => {}} />
                </Copy>
                <Button as='link' icon='delete' onClick={() => handleDelete(report.id, report.title)} />
            </div>,
        ]
    }

    return (
        <Card title={t('Reports')}>
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
            />
        </Card>
    )
}
