import useTranslation from '@/hooks/useTranslation'
import { Modal, ModalBody, ModalFooter, ModalHeader } from 'baseui/modal'
import React, { useEffect } from 'react'
import { useQueryDatastore } from '@/domain/datastore/hooks/useFetchDatastore'
import { Button } from '@/components/Button'
import { getWidget } from '../hooks/useSelector'
import { WidgetRenderer } from '../Widget/WidgetRenderer'
import WidgetEditForm from './WidgetForm'

export default function WidgetFormModel({
    store,
    handleFormSubmit,
    id: editWidgetId = '',
    isShow: isPanelModalOpen = false,
    setIsShow: setisPanelModalOpen,
}) {
    // @FIXME use event bus handle global state
    const [t] = useTranslation()
    const config = store(getWidget(editWidgetId)) ?? {}
    const [formData, setFormData] = React.useState({})

    const handleFormChange = (formData: any) => setFormData(formData)

    const type = formData?.chartType
    const tableName = Array.isArray(formData?.tableName) ? formData?.tableName[0] : formData?.tableName
    const filter = undefined
    const PAGE_TABLE_SIZE = 100

    const query = React.useMemo(
        () => ({
            tableName,
            start: 0,
            limit: PAGE_TABLE_SIZE,
            rawResult: true,
            ignoreNonExistingTable: true,
            // filter,
        }),
        [tableName]
    )

    const info = useQueryDatastore(query, false)

    useEffect(() => {
        if (tableName) info.refetch()
    }, [tableName, type])

    useEffect(() => {
        setFormData(config.fieldConfig?.data ?? {})
    }, [editWidgetId])

    // console.log('WidgetFormModel', query, info, editWidgetId)

    const formRef = React.useRef(null)

    return (
        <Modal
            isOpen={isPanelModalOpen}
            onClose={() => setisPanelModalOpen(false)}
            closeable
            animate
            autoFocus
            overrides={{
                Dialog: {
                    style: {
                        width: '90vw',
                        maxWidth: '1080px',
                        display: 'flex',
                        flexDirection: 'column',
                    },
                },
            }}
        >
            <ModalHeader>
                Add Panel
                {/* {editProject ? t('edit sth', [t('Project')]) : t('create sth', [t('Project')])} */}
            </ModalHeader>
            <ModalBody style={{ display: 'flex', gap: '30px' }}>
                <div
                    style={{
                        flexBasis: '600px',
                        flexGrow: '1',
                        maxHeight: '70vh',
                        minHeight: '348px',
                        height: 'auto',
                        overflow: 'auto',
                        backgroundColor: '#F7F8FA',
                        display: 'grid',
                        placeItems: 'center',
                        position: 'relative',
                    }}
                >
                    {!type && 'Select a metric to visalize in this chart'}
                    {type && (
                        <div
                            key={i}
                            style={{
                                width: '100%',
                                height: '100%',
                                overflow: 'auto',
                                padding: '20px 20px 20px',
                                backgroundColor: '#fff',
                                border: '1px solid #CFD7E6',
                                borderRadius: '4px',
                                position: 'relative',
                            }}
                        >
                            <WidgetRenderer type={type} data={info.data} />
                        </div>
                    )}
                </div>
                <WidgetEditForm
                    ref={formRef}
                    formData={formData}
                    onChange={handleFormChange}
                    onSubmit={handleFormSubmit}

                    // onSubmit={editProject ? handleEditProject : handleCreateProject}
                />
            </ModalBody>
            <ModalFooter>
                <div style={{ display: 'flex' }}>
                    <div style={{ flexGrow: 1 }} />
                    <Button
                        size='compact'
                        kind='secondary'
                        onClick={() => {
                            setisPanelModalOpen(false)
                        }}
                    >
                        {t('Cancel')}
                    </Button>
                    &nbsp;&nbsp;
                    <Button
                        size='compact'
                        onClick={() => {
                            formRef.current?.submit()
                            // setisPanelModalOpen(false)
                        }}
                    >
                        Submit
                    </Button>
                </div>
            </ModalFooter>
        </Modal>
    )
}
