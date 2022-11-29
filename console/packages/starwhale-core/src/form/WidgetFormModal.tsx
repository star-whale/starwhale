import useTranslation from '@/hooks/useTranslation'
import { Modal, ModalBody, ModalFooter, ModalHeader } from 'baseui/modal'
import React, { useEffect } from 'react'
import { useQueryDatastore } from '../datastore/hooks/useFetchDatastore'
import { Button } from '@/components/Button'
import { getWidget } from '../store/hooks/useSelector'
import { WidgetRenderer } from '../widget/WidgetRenderer'
import WidgetEditForm from './WidgetForm'
import useDatastoreTables from '../datastore/hooks/useDatastoreTables'
import { StoreType, useEditorContext } from '../context/EditorContextProvider'
import { useDatastoreTablesByPrefix } from '../datastore/hooks/useDatastoreTables'
import WidgetFormModel from './WidgetFormModel'
import useForceUpdate from '../utils/useForceUpdate'
import deepEqual from 'fast-deep-equal'
import { useDeepEffect } from '../../../../src/hooks/useDeepEffects'
import WidgetModel from '../widget/WidgetModel'

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
    // @FIXME use event bus handle global state
    const { dynamicVars } = useEditorContext()
    const { prefix } = dynamicVars
    const [t] = useTranslation()
    const config = store(getWidget(editWidgetId)) ?? {}
    const [formData, setFormData] = React.useState<Record<string, any>>({})
    const formRef = React.useRef(null)

    const handleFormChange = (formData: any) => {
        setFormData(formData)
    }

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

    const { tables } = useDatastoreTablesByPrefix(prefix)
    const info = useQueryDatastore(query)

    if (formData?.chartType && form?.widget?.type !== formData?.chartType) {
        form.setWidget(new WidgetModel({ type: formData.chartType }))
    }
    form.addDataTableNamesField(tables)
    form.addDataTableColumnsField(info.data?.columnTypes)

    useEffect(() => {
        setFormData(config.fieldConfig?.data ?? {})
    }, [editWidgetId])

    console.log('WidgetFormModel', form, formData)

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
                <WidgetEditForm
                    ref={formRef}
                    form={form}
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
                            // @ts-ignore
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
