import React from 'react'
import Card from '@/components/Card'
import { GridTable } from '@starwhale/ui/GridTable'
import { Modal, ModalBody, ModalHeader, ModalFooter } from 'baseui/modal'
import Button from '@starwhale/ui/Button'
import useTranslation from '@/hooks/useTranslation'
import EvalSelectForm from './EvalSelectForm'

function EvalSelectList() {
    const [editing, setEditing] = React.useState(false)
    const [isAddOpen, setIsAddOpen] = React.useState(false)
    const ref = React.useRef({})
    const [t] = useTranslation()

    const records = []
    const columnTypes = []

    return (
        <Card
            title=''
            style={{
                flexShrink: 1,
                marginBottom: 0,
                width: '100%',
                flex: 1,
                background: 'none',
                minHeight: '300px',
            }}
            bodyStyle={{
                flexDirection: 'column',
            }}
            extra={
                <Button kind='tertiary' onClick={() => setIsAddOpen(true)}>
                    {t('Add Evaluations')}
                </Button>
            }
        >
            <GridTable records={records} columnTypes={columnTypes} compareable={false} columnable />
            <Modal isOpen={isAddOpen} onClose={() => setIsAddOpen(false)} closeable animate autoFocus size='80%'>
                <ModalHeader>{t('Add Evaluations')}</ModalHeader>
                <ModalBody>
                    <EvalSelectForm ref={ref} />
                </ModalBody>
                <ModalFooter>
                    <div style={{ display: 'flex' }}>
                        <div style={{ flexGrow: 1 }} />
                        <Button
                            size='compact'
                            kind='secondary'
                            type='button'
                            onClick={() => {
                                setIsAddOpen(false)
                            }}
                        >
                            {t('Cancel')}
                        </Button>
                        &nbsp;&nbsp;
                        <Button
                            size='compact'
                            onClick={() => {
                                // @ts-ignore
                                // formRef.current?.submit()
                                console.log(ref.current.getData())
                            }}
                        >
                            {t('Confirm')}
                        </Button>
                    </div>
                </ModalFooter>
            </Modal>
        </Card>
    )
}
export { EvalSelectList }
export default EvalSelectList
