import { Modal, ModalBody, ModalFooter, ModalHeader } from 'baseui/modal'
import React, { useEffect } from 'react'
import Button from '@starwhale/ui/Button'
import { getWidget } from '../store/hooks/useSelector'
import { WidgetRenderer } from '../widget/WidgetRenderer'
import WidgetEditForm from './WidgetForm'
import { StoreType, useEditorContext } from '../context/EditorContextProvider'
import { useFetchDatastoreAllTables } from '../datastore/hooks/useFetchDatastoreAllTables'
import WidgetFormModel from './WidgetFormModel'
import WidgetModel from '../widget/WidgetModel'
import useTranslation from '@/hooks/useTranslation'
import useFetchDatastoreByTable from '../datastore/hooks/useFetchDatastoreByTable'
import useDatastorePage from '../datastore/hooks/useDatastorePage'

const PAGE_TABLE_SIZE = 100

export default function WidgetFormModal({
    store,
    handleFormSubmit,
    id: editWidgetId = '',
    isShow: isPanelModalOpen = false,
    setIsShow: setisPanelModalOpen = () => {},
    form,
}: {
    store: StoreType
    form: WidgetFormModel
    isShow?: boolean
    setIsShow?: any
    handleFormSubmit: (args: any) => void
    id?: string
}) {
    const [t] = useTranslation()
    // @FIXME use event bus handle global state
    const { dynamicVars } = useEditorContext()
    const { prefix } = dynamicVars
    const widgetIdSelector = React.useMemo(() => getWidget(editWidgetId) ?? {}, [editWidgetId])
    const config = store(widgetIdSelector)
    const [formData, setFormData] = React.useState<Record<string, any>>({})
    const formRef = React.useRef(null)

    const handleFormChange = (data: any) => {
        setFormData(data)
    }

    const type = formData?.chartType
    const tableName = Array.isArray(formData?.tableName) ? formData?.tableName[0] : formData?.tableName

    const { tables } = useFetchDatastoreAllTables(prefix)

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

    if (formData?.chartType && form?.widget?.type !== formData?.chartType) {
        form.setWidget(new WidgetModel({ type: formData.chartType }))
    }
    form.addDataTableNamesField(tables)
    form.addDataTableColumnsField(recordInfo.data?.columnTypes)

    useEffect(() => {
        if (config) setFormData(config.fieldConfig?.data ?? {})
        // eslint-disable-next-line react-hooks/exhaustive-deps
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
                        maxWidth: '1200px',
                        maxHeight: '640px',
                        display: 'flex',
                        flexDirection: 'column',
                    },
                },
            }}
        >
            <ModalHeader>{t('panel.chart.edit')}</ModalHeader>
            <ModalBody style={{ display: 'flex', gap: '30px', flex: 1, overflow: 'auto' }}>
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
                    {!type && t('panel.add.placeholder')}
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
                <WidgetEditForm
                    ref={formRef}
                    form={form}
                    formData={formData}
                    onChange={handleFormChange}
                    onSubmit={handleFormSubmit}
                />
            </ModalBody>
            <ModalFooter>
                <div style={{ display: 'flex' }}>
                    <div style={{ flexGrow: 1 }} />
                    <Button
                        size='compact'
                        kind='secondary'
                        type='button'
                        onClick={() => {
                            setisPanelModalOpen(false)
                        }}
                    >
                        Cancel
                    </Button>
                    &nbsp;&nbsp;
                    <Button
                        size='compact'
                        onClick={() => {
                            // @ts-ignore
                            formRef.current?.submit()
                        }}
                    >
                        Submit
                    </Button>
                </div>
            </ModalFooter>
        </Modal>
    )
}
