import { Modal, ModalBody, ModalFooter, ModalHeader } from 'baseui/modal'
import React, { useEffect } from 'react'
import { getWidget } from '../store/hooks/useSelector'
import { WidgetRenderer } from '../widget/WidgetRenderer'
import { StoreType } from '../context/EditorContextProvider'
import useFetchDatastoreByTable from '../datastore/hooks/useFetchDatastoreByTable'
import useDatastorePage from '../datastore/hooks/useDatastorePage'

const PAGE_TABLE_SIZE = 100

export default function WidgetPreviewModal({
    store,
    id: editWidgetId = '',
    isShow: isPanelModalOpen = false,
    setIsShow: setisPanelModalOpen = () => {},
}: {
    store: StoreType
    isShow?: boolean
    setIsShow?: any
    id?: string
}) {
    const widgetIdSelector = React.useMemo(() => getWidget(editWidgetId) ?? {}, [editWidgetId])
    const config = store(widgetIdSelector)
    const [formData, setFormData] = React.useState<Record<string, any>>({})

    const type = formData?.chartType
    const tableName = Array.isArray(formData?.tableName) ? formData?.tableName[0] : formData?.tableName

    const { getQueryParams } = useDatastorePage({
        pageNum: 1,
        pageSize: PAGE_TABLE_SIZE,
    })

    const { recordInfo, columnTypes, records } = useFetchDatastoreByTable(getQueryParams(tableName), !!tableName)

    const $data = React.useMemo(() => {
        if (!recordInfo.isSuccess) return { records: [], columnTypes: [] }
        return {
            records,
            columnTypes,
        }
    }, [recordInfo.isSuccess, records, columnTypes])

    useEffect(() => {
        if (config) setFormData(config.fieldConfig?.data ?? {})
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [config])

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
            <ModalHeader />
            <ModalBody style={{ display: 'flex', gap: '30px', flex: 1, overflow: 'auto' }}>
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
                                data={$data}
                                fieldConfig={{
                                    data: formData,
                                }}
                            />
                        </div>
                    )}
                </div>
            </ModalBody>
            <ModalFooter />
        </Modal>
    )
}
