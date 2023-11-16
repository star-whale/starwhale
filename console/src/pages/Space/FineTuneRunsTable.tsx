import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { ExtendButton } from '@starwhale/ui'
import { IFineTuneVo, IPageInfoFineTuneVo, api } from '@/api'
import Table from '@/components/Table'
import useFineTuneColumns from '@/domain/space/hooks/useFineTuneColumns'
import { useAccess } from '@/api/WithAuth'
import { FineTuneModelReleaseModal } from '@/domain/space/components/FineTuneModelReleaseForm'

export default function FineTuneRunsTable({
    data,
    isLoading,
    onView,
    onRefresh,
}: {
    data?: IPageInfoFineTuneVo
    isLoading?: boolean
    onView?: (id: number) => void
    onRefresh?: any
}) {
    const [t] = useTranslation()
    const isAccessRelease = useAccess('ft.run.release')
    const [releaseFT, setReleaseFT] = React.useState<IFineTuneVo | undefined>()
    const [isOpen, setIsOpen] = React.useState(false)

    const getActions = ({ id, ...rest }: IFineTuneVo) => [
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
        {
            access: isAccessRelease && rest.targetModel?.version?.draft,
            component: ({ hasText }) => (
                <ExtendButton
                    isFull
                    icon='Detail'
                    tooltip={!hasText ? t('ft.job.model.release') : undefined}
                    styleas={['menuoption', hasText ? undefined : 'highlight']}
                    onClick={() => {
                        setIsOpen(true)
                        setReleaseFT?.({
                            id,
                            ...rest,
                        })
                    }}
                >
                    {hasText ? t('ft.job.model.release') : undefined}
                </ExtendButton>
            ),
        },
    ]

    const { columns } = useFineTuneColumns()

    return (
        <>
            <Table
                renderActions={(rowIndex) => {
                    const ft = data?.list?.[rowIndex]
                    if (!ft) return undefined
                    return getActions(ft)
                }}
                isLoading={isLoading}
                columns={columns.map((v) => v.title)}
                data={data?.list?.map((ft) => {
                    // @ts-ignore
                    return columns.map((v) => <v.renderCell key={v.key} value={v.mapDataToValue?.(ft)} />)
                })}
            />
            <FineTuneModelReleaseModal data={releaseFT} isOpen={isOpen} setIsOpen={setIsOpen} onRefresh={onRefresh} />
        </>
    )
}
