import React from 'react'
import { Modal, ModalBody, ModalHeader, ModalFooter } from 'baseui/modal'
import Button from '@starwhale/ui/Button'
import useTranslation from '@/hooks/useTranslation'
import EvalSelectForm, { EvalSelectDataT } from './EvalSelectForm'
import { IconFont, Text } from '@starwhale/ui'
import { useDatastoreSummaryColumns } from '@starwhale/ui/GridDatastoreTable/hooks/useDatastoreSummaryColumns'
import GridTable from '@starwhale/ui/GridTable/GridTable'
import ToolBar from '@starwhale/ui/GridTable/components/ToolBar'
import _ from 'lodash'

const RenderButton = ({ count, editing, toggle }) => {
    const [t] = useTranslation()

    return (
        <div
            className='h-48px'
            style={{
                width: '240px',
                background: '#fff',
                borderTop: '1px solid #e2e7f0',
                borderLeft: '1px solid #e2e7f0',
                borderRight: '1px solid #e2e7f0',
                display: 'flex',
                justifyContent: 'space-between',
                padding: '0 20px',
                borderBottom: !editing ? '1px solid #e2e7f0' : '0px',
                borderRadius: editing ? '4px 4px 0 0' : '4px',
                marginBottom: '-1px',
                position: 'relative',
                zIndex: 2,
            }}
        >
            <p
                className='flex justify-center items-center text-14px gap-9px'
                style={{
                    color: '#02102B',
                    lineHeight: 1,
                    fontWeight: 600,
                }}
            >
                {t('evalution.panel.list')}

                <span
                    className='flex justify-center items-center text-12px'
                    style={{
                        lineHeight: 1,
                        fontWeight: 400,
                        minWidth: '28px',
                        height: '18px',
                        borderRadius: '12px',
                        backgroundColor: '#ebf1ff',
                    }}
                >
                    {count}
                </span>
            </p>
            <Button kind='tertiary' as='link' onClick={toggle}>
                <Text tooltip={editing ? t('Minimize') : t('Edit')} size='small' style={{ marginRight: '10px' }}>
                    <IconFont type={editing ? 'unfold21' : 'fold21'} />
                </Text>
            </Button>
        </div>
    )
}

function EvalSelectList() {
    const [editing, setEditing] = React.useState(false)
    const [isAddOpen, setIsAddOpen] = React.useState(false)
    const [selectData, setSelectData] = React.useState<EvalSelectDataT>({})
    const ref = React.useRef<{ getData: () => EvalSelectDataT }>()
    const [t] = useTranslation()

    const values = React.useMemo(() => {
        return Object.values(selectData ?? {})
    }, [selectData])

    console.log('final', values)

    const count = React.useMemo(() => {
        return values.reduce((acc, cur) => {
            return acc + (cur.rowSelectedIds?.length ?? 0)
        }, 0)
    }, [values])

    const uniconRecords = React.useMemo(
        () =>
            values.reduce((acc, cur) => {
                return [...acc, ...(cur.records ?? [])]
            }, []),
        [values]
    )

    const unionColumnTypes = React.useMemo(
        () =>
            values.reduce((acc, cur) => {
                return [...acc, ...(cur.columnTypes ?? [])]
            }, []),
        [values]
    )

    const $columns = useDatastoreSummaryColumns(unionColumnTypes as any)

    return (
        <div>
            {/* eval info button with minize/edit action */}
            <RenderButton
                count={count}
                editing={editing}
                toggle={() => {
                    setEditing(!editing)
                }}
            />

            {/* eval info list */}
            {editing && (
                <div
                    className='flex flex-column'
                    style={{
                        flexShrink: 1,
                        marginBottom: 0,
                        width: '100%',
                        flex: 1,
                        background: 'none',
                        minHeight: '400px',
                        maxHeight: '400px',
                        border: '1px solid #e2e7f0',
                        borderRadius: '0 4px 4px 4px',
                        padding: '18px 20px 10px',
                        position: 'relative',
                    }}
                >
                    <GridTable
                        queryable={false}
                        columnable
                        removable
                        compareable={false}
                        paginationable={false}
                        records={uniconRecords}
                        columnTypes={unionColumnTypes}
                        columns={$columns}
                        onRemove={(id) => {
                            setSelectData((prev) => {
                                const n = { ...prev }
                                Object.entries(n).forEach(([key, item]) => {
                                    if (item.rowSelectedIds.includes(id)) {
                                        item.rowSelectedIds.splice(item.rowSelectedIds.indexOf(id), 1)
                                        const filter = item.records.filter((record) => record.id.value !== id) ?? []
                                        if (filter.length === 0) {
                                            // @ts-ignore
                                            n[key] = undefined
                                        } else {
                                            // eslint-disable-next-line no-param-reassign
                                            item.records = filter
                                        }
                                    }
                                })
                                return _.pickBy(n, _.identity)
                            })
                        }}
                    >
                        <div className='flex gap-20px justify-end'>
                            <ToolBar columnable viewable={false} queryable={false} />
                            <div className='flex pb-8px'>
                                <Button kind='tertiary' onClick={() => setIsAddOpen(true)}>
                                    {t('evalution.panel.add')}
                                </Button>
                            </div>
                        </div>
                    </GridTable>
                    <Modal
                        isOpen={isAddOpen}
                        onClose={() => setIsAddOpen(false)}
                        closeable
                        animate
                        autoFocus
                        size='80%'
                    >
                        <ModalHeader>{t('evalution.panel.add')}</ModalHeader>
                        <ModalBody>
                            <EvalSelectForm ref={ref} initialSelectData={selectData} />
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
                                    {t('Done')}
                                </Button>
                                &nbsp;&nbsp;
                                <Button
                                    size='compact'
                                    onClick={() => {
                                        const next = ref.current?.getData()
                                        setSelectData((prev) => {
                                            console.log('confirm', prev, next)

                                            return _.pickBy(
                                                {
                                                    ...prev,
                                                    ...next,
                                                },
                                                _.identity
                                            )
                                        })
                                    }}
                                >
                                    {t('Confirm')}
                                </Button>
                            </div>
                        </ModalFooter>
                    </Modal>
                </div>
            )}
        </div>
    )
}
export { EvalSelectList }
export default EvalSelectList
