import React, { useCallback, useState } from 'react'
import Card from '@/components/Card'
import { usePage } from '@/hooks/usePage'
import useTranslation from '@/hooks/useTranslation'
import { Button, SIZE as ButtonSize } from 'baseui/button'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import Table from '@/components/Table'
import IconFont from '@/components/IconFont'
import { useFetchBaseImages } from '@/domain/runtime/hooks/useRuntime'
import { ICreateBaseImageSchema } from '@/domain/runtime/schemas/runtime'
import { createBaseImage, deleteBaseImage } from '@/domain/runtime/services/runtime'
import BaseImageForm from '@/domain/setting/components/BaseImageForm'
import { StyledLink } from 'baseui/link'
import { toaster } from 'baseui/toast'

export default function SettingBaseImageListCard() {
    const [page] = usePage()
    const baseImagesInfo = useFetchBaseImages(page)
    const [isCreateBaseImageOpen, setIsCreateBaseImageOpen] = useState(false)
    const handleCreateBaseImage = useCallback(
        async (data: ICreateBaseImageSchema) => {
            await createBaseImage(data)
            await baseImagesInfo.refetch()
            setIsCreateBaseImageOpen(false)
        },
        [baseImagesInfo]
    )
    const [t] = useTranslation()

    const handleAction = useCallback(
        async (imageId) => {
            await deleteBaseImage(imageId)
            toaster.positive(t('base image delete done'), { autoHideDuration: 2000 })
            await baseImagesInfo.refetch()
        },
        [baseImagesInfo, t]
    )

    return (
        <Card
            title={t('BaseImage')}
            titleIcon={undefined}
            extra={
                <Button
                    startEnhancer={<IconFont type='add' kind='white' />}
                    size={ButtonSize.compact}
                    onClick={() => setIsCreateBaseImageOpen(true)}
                >
                    {t('create')}
                </Button>
            }
        >
            <Table
                isLoading={baseImagesInfo.isLoading}
                columns={[t('sth name'), t('Action')]}
                data={
                    baseImagesInfo.data?.list?.map((baseImage) => {
                        return [
                            baseImage.name,
                            <StyledLink
                                animateUnderline={false}
                                className='row-center--inline gap4'
                                key={baseImage.id}
                                onClick={() => {
                                    handleAction(baseImage.id)
                                }}
                            >
                                {t('Delete')}
                            </StyledLink>,
                        ]
                    }) ?? []
                }
                // paginationProps={{
                //     start: baseImagesInfo.data?.pageNum,
                //     count: baseImagesInfo.data?.pageSize,
                //     total: baseImagesInfo.data?.total,
                //     afterPageChange: () => {
                //         baseImagesInfo.refetch()
                //     },
                // }}
            />
            <Modal
                isOpen={isCreateBaseImageOpen}
                onClose={() => setIsCreateBaseImageOpen(false)}
                closeable
                animate
                autoFocus
            >
                <ModalHeader>{t('create sth', [t('BaseImage')])}</ModalHeader>
                <ModalBody>
                    <BaseImageForm onSubmit={handleCreateBaseImage} />
                </ModalBody>
            </Modal>
        </Card>
    )
}
