import React from 'react'
import { Modal, ModalBody, ModalHeader, ModalFooter } from 'baseui/modal'
import Button from '@starwhale/ui/Button'
import useTranslation from '@/hooks/useTranslation'
import EvalSelectForm, { EvalSelectDataT } from './EvalSelectForm'
import { useControllableValue } from 'ahooks'
import { useProject } from '@/domain/project/hooks/useProject'
import useFineTuneEvaluation from '../hooks/useFineTuneEvaluation'

function EvalSelectImportList(props) {
    const { projectSummaryTableName } = useFineTuneEvaluation()
    const [, setValue] = useControllableValue<any>(props)
    const { project } = useProject()

    if (!project) return null

    return (
        <EvalSelectForm project={project} summaryTableName={projectSummaryTableName} onSelectedDataChange={setValue} />
    )
}

function EvalSelectExportList(props) {
    const { summaryTableName } = useFineTuneEvaluation()
    const [, setValue] = useControllableValue<any>(props)
    const { project } = useProject()

    if (!project) return null

    return <EvalSelectForm project={project} summaryTableName={summaryTableName} onSelectedDataChange={setValue} />
}

function EvalSelectListModal({
    isOpen = false,
    setIsOpen,
    type,
    title,
    onSubmit,
    ...props
}: {
    title?: React.ReactNode
    type?: 'import' | 'export'
    isOpen?: boolean
    setIsOpen?: (isOpen: boolean) => void
    value?: any[]
    onChange?: (data: any[]) => void
    onSubmit?: (data: any[]) => void
}) {
    const [t] = useTranslation()
    const [value, setValue] = useControllableValue<any>(props)
    return (
        <Modal isOpen={isOpen} onClose={() => setIsOpen?.(false)} closeable animate size='80%' returnFocus={false}>
            <ModalHeader>{title}</ModalHeader>
            <ModalBody>
                {type === 'import' ? (
                    <EvalSelectImportList value={value} onChange={setValue} />
                ) : (
                    <EvalSelectExportList value={value} onChange={setValue} />
                )}
            </ModalBody>
            <ModalFooter>
                <div style={{ display: 'flex' }}>
                    <div style={{ flexGrow: 1 }} />
                    <Button
                        size='compact'
                        kind='secondary'
                        type='button'
                        onClick={() => {
                            setIsOpen?.(false)
                        }}
                    >
                        {t('Cancel')}
                    </Button>
                    &nbsp;&nbsp;
                    <Button
                        size='compact'
                        onClick={() => {
                            setIsOpen?.(false)
                            onSubmit?.(value)
                        }}
                    >
                        {t('add')}
                    </Button>
                </div>
            </ModalFooter>
        </Modal>
    )
}
export { EvalSelectExportList, EvalSelectImportList, EvalSelectListModal }
