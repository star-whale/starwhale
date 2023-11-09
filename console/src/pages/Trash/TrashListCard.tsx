import React, { useEffect, useState } from 'react'
import Card from '@/components/Card'
import { usePage } from '@/hooks/usePage'
import useTranslation from '@/hooks/useTranslation'
import { useFetchTrashs } from '@/domain/trash/hooks/useFetchTrashs'
import { useStyletron } from 'baseui'
import { QueryInput } from '@starwhale/ui/Input'
import { ITrashSchema } from '@/domain/trash/schemas/trash'
import { useParams } from 'react-router-dom'
import { recoverTrash, removeTrash } from '@/domain/trash/services/trash'
import Table from '@/components/Table'
import { formatTimestampDateTime } from '@/utils/datetime'
import { getReadableStorageQuantityStr } from '@/utils'
import { ConfirmButton } from '@starwhale/ui/Modal'
import { toaster } from 'baseui/toast'

export default function TrashListCard() {
    const [page] = usePage()
    const { projectId } = useParams<{ trashId: string; projectId: string }>()
    const info = useFetchTrashs(projectId, page)
    const [filter, setFilter] = useState('')
    const [data, setData] = useState<ITrashSchema[]>([])
    const [css] = useStyletron()
    const [t] = useTranslation()

    useEffect(() => {
        const items = info.data?.list ?? []
        setData(
            items.filter((i) => {
                if (filter) return [i.name, i.trashedBy, i.type].join('/').includes(filter)
                return filter === ''
            })
        )
    }, [filter, info.data])

    const getActions = (trash: ITrashSchema) => [
        {
            access: true,
            quickAccess: true,
            component: ({ hasText }) => (
                <ConfirmButton
                    title={t('trash.restore.confirm')}
                    icon='Restore'
                    styleas={['menuoption', hasText ? undefined : 'highlight']}
                    tooltip={!hasText ? t('trash.restore.button') : undefined}
                    onClick={async () => {
                        await recoverTrash(projectId, trash.id)
                        toaster.positive(t('trash.restore.success'), { autoHideDuration: 1000 })
                        await info.refetch()
                    }}
                >
                    {hasText ? t('trash.restore.button') : undefined}
                </ConfirmButton>
            ),
        },
        {
            access: true,
            quickAccess: true,
            component: ({ hasText }) => (
                <ConfirmButton
                    icon='delete'
                    styleas={['menuoption', hasText ? undefined : 'highlight', 'negative']}
                    title={t('trash.remove.confirm')}
                    tooltip={!hasText ? t('trash.remove.button') : undefined}
                    onClick={async () => {
                        await removeTrash(projectId, trash.id)
                        toaster.positive(t('trash.remove.success'), { autoHideDuration: 1000 })
                        await info.refetch()
                    }}
                >
                    {hasText ? t('trash.remove.button') : undefined}
                </ConfirmButton>
            ),
        },
    ]

    return (
        <Card title={t('trash.title')}>
            <div className={css({ marginBottom: '20px', width: '280px' })}>
                <QueryInput
                    onChange={(val: string) => {
                        setFilter(val.trim())
                    }}
                />
            </div>
            <Table
                renderActions={(rowIndex) => {
                    const trash = data[rowIndex]
                    if (!trash) return undefined
                    return getActions(trash)
                }}
                isLoading={info.isLoading}
                columns={[
                    t('trash.name'),
                    t('trash.type'),
                    t('trash.trashedTime'),
                    t('trash.size'),
                    t('trash.trashedBy'),
                    t('trash.updatedTime'),
                    t('trash.retentionTime'),
                ]}
                data={
                    data?.map((trash) => {
                        return [
                            trash.name,
                            trash.type,
                            trash.trashedTime && formatTimestampDateTime(trash.trashedTime),
                            trash.size && getReadableStorageQuantityStr(trash.size),
                            trash.trashedBy,
                            trash.lastUpdatedTime && formatTimestampDateTime(trash.lastUpdatedTime),
                            trash.retentionTime && formatTimestampDateTime(trash.retentionTime),
                        ]
                    }) ?? []
                }
                paginationProps={{
                    start: info.data?.pageNum,
                    count: info.data?.pageSize,
                    total: info.data?.total,
                    afterPageChange: () => {
                        info.refetch()
                    },
                }}
            />
        </Card>
    )
}
