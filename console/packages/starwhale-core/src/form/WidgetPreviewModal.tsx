import useTranslation from '@/hooks/useTranslation'
import { Modal, ModalBody, ModalFooter, ModalHeader } from 'baseui/modal'
import React, { useEffect } from 'react'
import { useQueryDatastore } from '../datastore/hooks/useFetchDatastore'
import { Button } from '@/components/Button'
import { getWidget } from '../store/hooks/useSelector'
import { WidgetRenderer } from '../widget/WidgetRenderer'
import { StoreType, useEditorContext } from '../context/EditorContextProvider'
import WidgetFormModel from './WidgetFormModel'

const PAGE_TABLE_SIZE = 100

export default function WidgetPreviewModal({
    store,
    id: editWidgetId = '',
    isShow: isPanelModalOpen = false,
    setIsShow: setisPanelModalOpen = () => {},
}: {
    store: StoreType
    form: WidgetFormModel
    isShow?: boolean
    setIsShow?: any
    handleFormSubmit: (args: any) => void
    id?: string
}) {
    // @FIXME use event bus handle global state
    const { dynamicVars } = useEditorContext()
    const { prefix } = dynamicVars
    const [t] = useTranslation()
    const config = store(getWidget(editWidgetId)) ?? {}
    const [formData, setFormData] = React.useState<Record<string, any>>({})

    const type = formData?.chartType
    const query = React.useMemo(() => {
        // @ts-ignore
        const tableName = Array.isArray(formData?.tableName) ? formData?.tableName[0] : formData?.tableName
        return {
            tableName,
            start: 0,
            limit: PAGE_TABLE_SIZE,
            rawResult: true,
            ignoreNonExistingTable: true,
        }
    }, [formData?.tableName])

    const info = useQueryDatastore(query)

    useEffect(() => {
        setFormData(config.fieldConfig?.data ?? {})
    }, [editWidgetId])

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
                        height: '90vh',
                        maxWidth: '1200px',
                        maxHeight: '640px',
                        display: 'flex',
                        flexDirection: 'column',
                    },
                },
            }}
        >
            <ModalHeader></ModalHeader>
            <ModalBody style={{ display: 'flex', gap: '30px', flex: 1 }}>
                <div
                    style={{
                        flexGrow: '1',
                        minHeight: '348px',
                        height: '100%',
                        overflow: 'auto',
                        backgroundColor: '#F7F8FA',
                        display: 'grid',
                        placeItems: 'center',
                        position: 'relative',
                    }}
                >
                    {type && (
                        <div
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
                            {/* @ts-ignore */}
                            <WidgetRenderer
                                type={type}
                                data={info.data}
                                fieldConfig={{
                                    data: formData,
                                }}
                            />
                        </div>
                    )}
                </div>
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
                </div>
            </ModalFooter>
        </Modal>
    )
}
