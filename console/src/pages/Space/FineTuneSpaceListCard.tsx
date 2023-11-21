import React from 'react'
import Card from '@/components/Card'
import { usePage } from '@/hooks/usePage'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import Table from '@/components/Table'
import { useHistory, useParams } from 'react-router-dom'
import User from '@/domain/user/components/User'
import { Button, ExtendButton } from '@starwhale/ui'
import { useAccess } from '@/api/WithAuth'
import { IFineTuneSpaceVo, api } from '@/api'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import SpaceForm from '@/domain/space/components/SpaceForm'
import { useEventCallback } from '@starwhale/core'

export default function FineTuneSpaceListCard() {
    const [page] = usePage()
    const history = useHistory()
    const { projectId } = useParams<{ projectId: any }>()
    const [isOpen, setIsOpen] = React.useState(false)
    const [editRow, setEditRow] = React.useState<IFineTuneSpaceVo>()

    const info = api.useListSpace(projectId, {
        ...page,
    })

    const [t] = useTranslation()

    const isAccessCreate = useAccess('ft.space.create')
    const isAccessUpdate = useAccess('ft.space.update')
    // const isAccessDelete = useAccess('ft.space.delete')

    const getActions = (data: IFineTuneSpaceVo) => [
        {
            access: true,
            quickAccess: true,
            component: ({ hasText }) => (
                <ExtendButton
                    isFull
                    icon='Detail'
                    tooltip={!hasText ? t('View Details') : undefined}
                    styleas={['menuoption', hasText ? undefined : 'highlight']}
                    onClick={() => history.push(`/projects/${projectId}/spaces/${data.id}`)}
                >
                    {hasText ? t('View Details') : undefined}
                </ExtendButton>
            ),
        },
        {
            access: isAccessUpdate,
            component: ({ hasText }) => (
                <ExtendButton
                    isFull
                    icon='edit'
                    tooltip={!hasText ? t('Edit') : undefined}
                    styleas={['menuoption', hasText ? undefined : 'highlight']}
                    onClick={() => {
                        setEditRow(data)
                        setIsOpen(true)
                    }}
                >
                    {hasText ? t('ft.space.action.edit') : undefined}
                </ExtendButton>
            ),
        },
    ]

    const handleCreate = useEventCallback(async (data: IFineTuneSpaceVo) => {
        await api.createSpace(projectId, {
            name: data.name as string,
            description: data.description as string,
        })
        await info.refetch()
        setIsOpen(false)
    })
    const handleEdit = useEventCallback(async ({ id, ...data }: IFineTuneSpaceVo) => {
        if (!id) return
        await api.updateSpace(projectId, id, {
            name: data.name as string,
            description: data.description as string,
        })
        await info.refetch()
        setIsOpen(false)
    })

    return (
        <Card
            title={t('ft.space.title')}
            className='content-full'
            extra={
                isAccessCreate && (
                    <Button
                        size='compact'
                        onClick={() => {
                            setEditRow(undefined)
                            setIsOpen(true)
                        }}
                    >
                        {t('create')}
                    </Button>
                )
            }
        >
            <Table
                renderActions={(rowIndex) => {
                    const data = info.data?.list?.[rowIndex]
                    if (!data) return undefined
                    return getActions(data)
                }}
                isLoading={info.isLoading}
                columns={[t('ft.space.id'), t('ft.space.name'), t('Owner'), t('Created'), t('Description')]}
                data={
                    info.data?.list?.map((data) => {
                        return [
                            data.id,
                            data.name,
                            data.owner && <User user={data.owner} />,
                            data.createdTime && formatTimestampDateTime(data.createdTime),
                            data.description,
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
            <Modal isOpen={isOpen} onClose={() => setIsOpen(false)} closeable animate autoFocus>
                <ModalHeader>
                    {editRow ? t('edit sth', [t('ft.space.title')]) : t('create sth', [t('ft.space.title')])}
                </ModalHeader>
                <ModalBody>
                    <SpaceForm
                        label={t('ft.space.title')}
                        data={editRow}
                        onSubmit={editRow ? handleEdit : handleCreate}
                    />
                </ModalBody>
            </Modal>
        </Card>
    )
}
