import React, { useCallback, useState } from 'react'
import { Button } from 'baseui/button'
import { toaster } from 'baseui/toast'
import { StatefulPopover, PLACEMENT } from 'baseui/popover'
import { StatefulMenu } from 'baseui/menu'
import { AiFillCaretDown } from 'react-icons/ai'
import { createUseStyles } from 'react-jss'
import useTranslation from '@/hooks/useTranslation'
import { Modal, ModalBody, ModalHeader } from 'baseui/modal'
import ProjectFrom, { IProjectFormProps } from '@project/components/ProjectForm'
import { createProject } from '@project/services/project'
import { ICreateProjectSchema } from '@project/schemas/project'
import { useHistory } from 'react-router-dom'

export default function HeaderLeftMenu() {
    const [t] = useTranslation()
    const [isCreateProjectModalOpen, setIsCreateProjectModalOpen] = useState(false)
    const handleCreateProject = useCallback(async (data: ICreateProjectSchema) => {
        await createProject(data)
        setIsCreateProjectModalOpen(false)
        toaster.positive(t('project created'), { autoHideDuration: 2000 })
    }, [])

    const history = useHistory()

    const PROJECT_ITEMS = [
        { label: t('Create Project'), type: 'create' },
        { label: t('Project List'), type: 'list' },
    ]
    // const USER_ITEMS = [
    //     { label: t('Create User'), type: 'create' },
    //     { label: t('User List'), type: 'list' },
    // ]

    return (
        <div>
            <StatefulPopover
                focusLock
                placement={PLACEMENT.bottomLeft}
                content={({ close }) => (
                    <StatefulMenu
                        items={PROJECT_ITEMS}
                        onItemSelect={({ item }) => {
                            if (item.type == 'create') {
                                setIsCreateProjectModalOpen(true)
                            } else if (item.type == 'list') {
                                history.push('/projects')
                            }
                            close()
                        }}
                        overrides={{
                            List: { style: { height: '150px', width: '150px' } },
                        }}
                    />
                )}
            >
                <Button
                    overrides={{
                        BaseButton: {
                            style: ({ $theme }) => ({
                                'backgroundColor': 'transparent',
                                ':hover': {
                                    backgroundColor: 'transparent',
                                },
                            }),
                        },
                    }}
                    endEnhancer={() => <AiFillCaretDown size={24} />}
                >
                    {t('PROJECT')}
                </Button>
            </StatefulPopover>
            {/* <StatefulPopover
                placement={PLACEMENT.bottomLeft}
                content={({ close }) => (
                    <StatefulMenu
                        items={USER_ITEMS}
                        onItemSelect={({ item }) => {
                            if (item.type == 'create') {
                            } else if (item.type == 'list') {
                                history.push('/users')
                            }
                            close()
                        }}
                        overrides={{
                            List: { style: { height: '100px', width: '125px' } },
                        }}
                    />
                )}
            >
                <Button
                    overrides={{
                        BaseButton: {
                            style: ({ $theme }) => ({
                                'backgroundColor': 'transparent',
                                ':hover': {
                                    backgroundColor: 'transparent',
                                },
                            }),
                        },
                    }}
                    endEnhancer={() => <AiFillCaretDown size={24} />}
                >
                    {t('USER')}
                </Button>
            </StatefulPopover> */}
            <Modal
                isOpen={isCreateProjectModalOpen}
                onClose={() => setIsCreateProjectModalOpen(false)}
                closeable
                animate
                autoFocus
            >
                <ModalHeader>{t('Project')}</ModalHeader>
                <ModalBody>
                    <ProjectFrom onSubmit={handleCreateProject} />
                </ModalBody>
            </Modal>
        </div>
    )
}
