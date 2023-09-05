import React from 'react'
import { Modal, ModalBody, ModalHeader, ModalFooter } from 'baseui/modal'
import Button, { ExtendButton } from '@starwhale/ui/Button'
import useTranslation from '@/hooks/useTranslation'
import EvalSelectForm, { EvalSelectDataT } from './EvalSelectForm'
import { IconFont, Text } from '@starwhale/ui'
import { useDatastoreSummaryColumns } from '@starwhale/ui/GridDatastoreTable/hooks/useDatastoreSummaryColumns'
import GridTable from '@starwhale/ui/GridTable/GridTable'
import ToolBar from '@starwhale/ui/GridTable/components/ToolBar'
import _ from 'lodash'
import { TextLink } from '../Link'
import { CustomColumn } from '@starwhale/ui/base/data-table'
import { useStore } from '@starwhale/core/store'
import { WidgetStoreState, getTableDistinctColumnTypes } from '@starwhale/core'
import { toaster } from 'baseui/toast'

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
            <ExtendButton
                noPadding
                kind='tertiary'
                as='link'
                onClick={toggle}
                overrides={{
                    BaseButton: {
                        style: {
                            'color': 'rgba(2,16,43,0.60)',
                            ':hover': {
                                color: '#5181E0',
                            },
                            ':focus': {
                                color: '#1C4CAD',
                            },
                            ':active': {
                                color: '#1C4CAD',
                            },
                        },
                    },
                }}
            >
                <Text tooltip={editing ? t('Minimize') : t('Edit')} size='small' style={{ color: 'inherit' }}>
                    <IconFont type={editing ? 'arrow_down' : 'arrow_top'} size={18} style={{ color: 'inherit' }} />
                </Text>
            </ExtendButton>
        </div>
    )
}

const selector = (state: WidgetStoreState) => ({
    isEditable: state.isEditable,
})

function EvalSelectList({
    editing,
    value,
    onEditingChange,
    onSelectDataChange,
    currentView,
    onCurrentViewChange,
}: {
    editing?: boolean
    onEditingChange?: (editing: boolean) => void
    value?: EvalSelectDataT
    onSelectDataChange?: (data: EvalSelectDataT) => void
    currentView?: any
    onCurrentViewChange?: (data: any) => void
}) {
    const [isAddOpen, setIsAddOpen] = React.useState(false)
    const ref = React.useRef<{ getData: () => EvalSelectDataT }>()
    const [t] = useTranslation()

    const selectData = value

    const values = React.useMemo(() => {
        return Object.values(selectData ?? {})
    }, [selectData])

    const count = React.useMemo(() => {
        return values.reduce((acc, cur) => {
            return acc + (cur.records?.length ?? 0)
        }, 0)
    }, [values])

    const uniconRecords = React.useMemo(
        () =>
            values.reduce((acc, cur) => {
                // FIXME hacked add projectid to records
                return [...acc, ...(cur.records ?? []).map((record) => ({ ...record, projectId: cur.projectId }))]
            }, []),
        [values]
    )

    const unionColumnTypes = React.useMemo(() => getTableDistinctColumnTypes(selectData), [selectData])

    const $columns = useDatastoreSummaryColumns(unionColumnTypes as any)

    // overrider sys/id use inline projectId
    const $override = React.useMemo(
        () =>
            $columns.map((column) => {
                if (column.key === 'sys/id')
                    return CustomColumn<any, any>({
                        ...column,
                        renderCell: (props: any) => {
                            const { value: id, record } = props.value || {}
                            if (!id) return <></>
                            return (
                                <TextLink
                                    target='_blank'
                                    to={`/projects/${record?.projectId}/evaluations/${id}/results`}
                                >
                                    {id}
                                </TextLink>
                            )
                        },
                    })

                return column
            }),
        [$columns]
    )

    const { isEditable } = useStore(selector)
    const editable = isEditable?.()
    // const isEditable = useStoreApi().getState().isEditable()
    const AddButton = React.useMemo(() => {
        if (!editable) return null

        return (
            <div className='flex pb-8px'>
                <Button kind='tertiary' onClick={() => setIsAddOpen(true)}>
                    {t('evalution.panel.add')}
                </Button>
            </div>
        )
    }, [editable, t])

    return (
        <div>
            {/* eval info button with minize/edit action */}
            <RenderButton
                count={count}
                editing={editing}
                toggle={() => {
                    onEditingChange?.(!editing)
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
                        backgroundColor: '#FFF',
                    }}
                >
                    <GridTable
                        queryable={false}
                        columnable={editable}
                        removable={editable}
                        compareable={false}
                        paginationable={false}
                        queryinline={false}
                        sortable
                        columnleinline={false}
                        // @ts-ignore
                        records={uniconRecords}
                        columnTypes={unionColumnTypes}
                        columns={$override}
                        onRemove={(id) => {
                            const renew = (prev: EvalSelectDataT) => {
                                const n = { ...prev }
                                Object.entries(n).forEach(([key, item]) => {
                                    const filter = item.records.filter((record) => record.id.value !== id) ?? []
                                    if (filter.length === 0) {
                                        // @ts-ignore
                                        n[key] = undefined
                                    } else {
                                        n[key].records = filter
                                    }
                                })
                                return _.pickBy(n, _.identity)
                            }
                            const next = renew(selectData || {})
                            onSelectDataChange?.(next)
                        }}
                        currentView={currentView}
                        onCurrentViewChange={(s) => onCurrentViewChange?.(s.currentView)}
                    >
                        <div className='flex gap-20px justify-end'>
                            <ToolBar columnable={editable} viewable={false} queryable={false} />
                            {AddButton}
                        </div>
                    </GridTable>
                    <Modal
                        isOpen={isAddOpen}
                        onClose={() => setIsAddOpen(false)}
                        closeable
                        animate
                        size='80%'
                        returnFocus={false}
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
                                    {t('Cancel')}
                                </Button>
                                &nbsp;&nbsp;
                                <Button
                                    size='compact'
                                    onClick={() => {
                                        const next = ref.current?.getData()
                                        // eslint-disable-next-line consistent-return
                                        const renew = _.mergeWith({}, selectData, next, (objValue, srcValue, key) => {
                                            if (_.isArray(objValue)) {
                                                if (key === 'records') {
                                                    return _.unionBy(objValue.concat(srcValue), 'id.value')
                                                }
                                                return _.union(objValue.concat(srcValue))
                                            }
                                        })
                                        onSelectDataChange?.(renew)
                                        setIsAddOpen(false)
                                        const num = Object.values(renew).reduce((acc, cur) => {
                                            return acc + (cur.records?.length ?? 0)
                                        }, 0)
                                        toaster.positive(t('evalution.panel.add.desc', [num]))
                                    }}
                                >
                                    {t('add')}
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
