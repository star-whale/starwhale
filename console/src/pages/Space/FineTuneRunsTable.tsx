import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { ExtendButton } from '@starwhale/ui'
import { IFineTuneVo, IPageInfoFineTuneVo, api } from '@/api'
import Table from '@/components/Table'
import useFineTuneColumns from '@/domain/space/hooks/useFineTuneColumns'

export default function FineTuneRunsTable({
    data,
    isLoading,
    onView,
}: {
    data?: IPageInfoFineTuneVo
    isLoading?: boolean
    onView?: (id: number) => void
}) {
    const [t] = useTranslation()
    const getActions = ({ job, id }: IFineTuneVo) => [
        {
            access: true,
            quickAccess: true,
            component: ({ hasText }) => (
                <ExtendButton
                    isFull
                    icon='Detail'
                    tooltip={!hasText ? t('View Details') : undefined}
                    styleas={['menuoption', hasText ? undefined : 'highlight']}
                    onClick={() => onView?.(id)}
                >
                    {hasText ? t('View Details') : undefined}
                </ExtendButton>
            ),
        },
    ]

    const { columns } = useFineTuneColumns()

    return (
        <Table
            renderActions={(rowIndex) => {
                const ft = data?.list?.[rowIndex]
                if (!ft) return undefined
                return getActions(ft)
            }}
            isLoading={isLoading}
            columns={columns.map((v) => v.title)}
            data={data?.list?.map((ft) => {
                return columns.map((v) => <v.renderCell key={v.key} value={v.mapDataToValue?.(ft)} />)
            })}
        />
    )
}
