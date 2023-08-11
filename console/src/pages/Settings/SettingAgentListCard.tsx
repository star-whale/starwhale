import React, { useCallback } from 'react'
import Card from '@/components/Card'
import { usePage } from '@/hooks/usePage'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import Table from '@/components/Table'
import { useFetchAgents } from '@/domain/setting/hooks/useSettings'
import { StyledLink } from 'baseui/link'
import { deleteAgent } from '@/domain/setting/services/system'
import { toaster } from 'baseui/toast'
import { AgentStatus } from '@/domain/job/schemas/agent'

export default function SettingAgentListCard() {
    const [page] = usePage()
    const agentsInfo = useFetchAgents(page)
    const [t] = useTranslation()

    const handleAction = useCallback(
        async (serialNumber: string) => {
            await deleteAgent(serialNumber)
            toaster.positive(t('agent delete done'), { autoHideDuration: 2000 })
            await agentsInfo.refetch()
        },
        [agentsInfo, t]
    )

    return (
        <Card title={t('Agent')} titleIcon={undefined}>
            <Table
                isLoading={agentsInfo.isLoading}
                columns={[t('IP'), t('UUID'), t('Status'), t('Version'), t('Connected Time'), t('Action')]}
                data={
                    agentsInfo.data?.list?.map((agent) => {
                        return [
                            agent.ip,
                            agent.serialNumber,
                            agent.status,
                            agent.version,
                            agent.connectedTime > 0 ? formatTimestampDateTime(agent.connectedTime) : '-',
                            AgentStatus.OFFLINE === agent.status ? (
                                <StyledLink
                                    animateUnderline={false}
                                    className='row-center--inline gap4'
                                    key={agent.serialNumber}
                                    onClick={() => {
                                        handleAction(agent.serialNumber)
                                    }}
                                >
                                    {t('Delete')}
                                </StyledLink>
                            ) : (
                                '-'
                            ),
                        ]
                    }) ?? []
                }
                paginationProps={{
                    start: agentsInfo.data?.pageNum,
                    count: agentsInfo.data?.pageSize,
                    total: agentsInfo.data?.total,
                    afterPageChange: () => {
                        agentsInfo.refetch()
                    },
                }}
            />
        </Card>
    )
}
