import React, { useCallback, useState } from 'react'
import Table from '@/components/Table'
import useTranslation from '@/hooks/useTranslation'
import { useProject, useProjectLoading } from '@project/hooks/useProject'
import Card from '@/components/Card'
import { formatTimestampDateTime } from '@/utils/datetime'
import User from '@/domain/user/components/User'
import { Button, SIZE as ButtonSize } from 'baseui/button'
import { ICreateProjectSchema } from '@project/schemas/project'
import { createProject } from '@project/services/project'
import { Modal, ModalBody, ModalHeader } from 'baseui/modal'
import ProjectForm from '@project/components/ProjectForm'

export default function ProjectOverview() {
    const { project } = useProject()
    const { projectLoading } = useProjectLoading()

    const [isCreateProjectModalOpen, setIsCreateProjectModalOpen] = useState(false)
    const handleCreateProject = useCallback(async (data: ICreateProjectSchema) => {
        await createProject(data)
        setIsCreateProjectModalOpen(false)
    }, [])

    const [t] = useTranslation()

    return (
        <Card
            title={t('overview')}
            titleIcon={undefined}
            extra={
                <Button size={ButtonSize.compact} onClick={() => setIsCreateProjectModalOpen(true)}>
                    {t('create')}
                </Button>
            }
        >
            <Table
                isLoading={projectLoading}
                columns={[t('Project Name'), t('Created'), t('Owner')]}
                data={[
                    [
                        project?.name,
                        project?.owner && <User user={project?.owner} />,
                        project?.createTime && formatTimestampDateTime(project.createTime),
                    ],
                ]}
            />
            <Modal
                isOpen={isCreateProjectModalOpen}
                onClose={() => setIsCreateProjectModalOpen(false)}
                closeable
                animate
                autoFocus
            >
                <ModalHeader>{t('Project')}</ModalHeader>
                <ModalBody>
                    <ProjectForm onSubmit={handleCreateProject} />
                </ModalBody>
            </Modal>
        </Card>
    )
}
