import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { ExtendButton } from '@starwhale/ui'
import { IFineTuneVo } from '@/api'
import Table from '@/components/Table'
import useFineTuneColumns from '@/domain/space/hooks/useFineTuneColumns'
import { useAccess } from '@/api/WithAuth'
import { FineTuneModelReleaseModal } from '@/domain/space/components/FineTuneModelReleaseForm'
import { useFineTuneJobActions, IFineTuneJobActionParams } from '@/domain/space/components/FineTuneJobActionGroup'

export default function FineTuneRunsTable({
    list,
    isLoading,
    onView,
    onRefresh,
    params,
}: {
    list: IFineTuneVo[]
    isLoading?: boolean
    onView?: (id: number) => void
    onRefresh?: any
    params?: IFineTuneJobActionParams
}) {
    const [t] = useTranslation()
    const isAccessRelease = useAccess('ft.run.release')
    const [releaseFT, setReleaseFT] = React.useState<IFineTuneVo | undefined>()
    const [isOpen, setIsOpen] = React.useState(false)
    const { getActions: _getActions } = useFineTuneJobActions({ onRefresh })
    const { projectId, spaceId } = params ?? {}

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
            quickAccess: true,
            component: ({ hasText }) => (
                <ExtendButton
                    isFull
                    icon='release'
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
        ..._getActions({ projectId, spaceId, fineTuneId: String(id), job: rest.job, jobId: rest.job?.id }),
    ]

    const { columns, renderCell } = useFineTuneColumns()

    return (
        <>
            <Table
                renderActions={(rowIndex) => {
                    const ft = list?.[rowIndex]
                    if (!ft) return undefined
                    return getActions(ft)
                }}
                isLoading={isLoading}
                columns={columns.map((v) => v.title)}
                data={list?.map((ft) => {
                    // @ts-ignore
                    return columns.map((v) => renderCell(ft)(v.key))
                })}
            />
            <FineTuneModelReleaseModal data={releaseFT} isOpen={isOpen} setIsOpen={setIsOpen} onRefresh={onRefresh} />
        </>
    )
}
