import React, { useMemo, useCallback, useRef, useEffect } from 'react'
import { StyledDropdownListItem } from 'baseui/select'
import Select from '@/components/Select'
import { StyledList, StyledEmptyState, OptionListProps } from 'baseui/menu'
import { Modal, ModalBody, ModalFooter, ModalHeader } from 'baseui/modal'
import useTranslation from '@/hooks/useTranslation'
import _ from 'lodash'
import { toaster } from 'baseui/toast'
import type { ColumnT, ConfigT } from './types'
import ViewList from './config-views-list'
import ViewsEdit from './config-views-edit'
import { ITableState, IStore } from './store'
import Button from '../../Button'

type PropsT = {
    columns: ColumnT[]
    rows: any[]
    store: ITableState
    useStore: IStore
}

const ALLRUNS = 'all'

function ConfigViews(props: PropsT) {
    const [t] = useTranslation()

    const { store, useStore } = props
    const [isManageViewOpen, setIsManageViewOpen] = React.useState(false)
    const [selectId, setSelectId] = React.useState(store.currentView?.id ?? '')

    useEffect(() => {
        if (store.currentView) {
            setSelectId(store.currentView?.id ?? '')
        }
    }, [store.currentView])

    const $options: any = useMemo(() => {
        return [
            ...store.views
                .filter((v) => v.isShow)
                .map((v) => ({
                    id: v.id,
                    label: v.name,
                })),
            {
                id: ALLRUNS,
                label: t('All runs'),
            },
        ]
    }, [store.views, t])

    const viewListRef = useRef(null)
    const viewRef = useRef(null)

    const handleEdit = useCallback(
        (view) => {
            store.onShowViewModel(true, view)
        },
        [store]
    )

    return (
        <div className='table-config-view' style={{ maxWidth: '280px' }}>
            <Select
                size='compact'
                options={$options}
                placeholder={t('Select a view')}
                clearable={false}
                overrides={{
                    DropdownContainer: {
                        style: {
                            maxHeight: '40vh',
                        },
                    },
                    Dropdown: {
                        props: {
                            setIsAddViewOpen: () => store.onShowViewModel(true, null),
                            setIsManageViewOpen: () => setIsManageViewOpen(true),
                        },
                        // eslint-disable-next-line @typescript-eslint/no-use-before-define
                        component: ConfigViewDropdown,
                    },
                }}
                onChange={(params) => {
                    const id = params.option?.id as string
                    if (id === ALLRUNS) {
                        useStore.setState({
                            currentView: {
                                filters: [],
                                selectedIds: props.columns.map((v) => v.key),
                                pinnedIds: [],
                                sortedIds: [],
                                sortBy: '',
                                id: ALLRUNS,
                            },
                        })
                        return
                    }

                    setSelectId(id)
                    const view = store.views.find((v) => v.id === id)
                    useStore.setState({ currentView: view as ConfigT })
                }}
                value={selectId ? [{ id: selectId }] : []}
            />
            <Modal
                isOpen={store.viewModelShow}
                onClose={() => store.onShowViewModel(false, null)}
                closeable
                animate
                autoFocus
                overrides={{
                    Dialog: {
                        style: {
                            width: '700px',
                            display: 'flex',
                            flexDirection: 'column',
                        },
                    },
                }}
            >
                <ModalHeader>{!store.viewEditing?.id ? t('Add a New View') : t('Edit View')}</ModalHeader>
                <ModalBody style={{ flex: '1 1 0', display: 'flex', flexDirection: 'column', gap: '20px' }}>
                    <ViewsEdit
                        ref={viewRef}
                        view={store.viewEditing}
                        columns={props.columns ?? []}
                        rows={props.rows ?? []}
                    />
                </ModalBody>
                <ModalFooter>
                    <Button
                        type='submit'
                        onClick={(event) => {
                            event.preventDefault()
                            const newView = (viewRef.current as any).getView()
                            if (store.checkDuplicateViewName(newView.name, newView.id)) {
                                toaster.negative('View name already exists', { autoHideDuration: 2000 })
                                return
                            }
                            store.onViewUpdate(newView)
                            store.onShowViewModel(false, null)
                        }}
                    >
                        Apply
                    </Button>
                </ModalFooter>
            </Modal>

            <Modal
                isOpen={isManageViewOpen}
                onClose={() => setIsManageViewOpen(false)}
                closeable
                animate
                autoFocus
                overrides={{
                    Dialog: {
                        style: {
                            width: 'fit-content',
                            display: 'flex',
                            flexDirection: 'column',
                        },
                    },
                }}
            >
                <ModalHeader>{t('Manage Views')}</ModalHeader>
                <ModalBody>
                    <ViewList ref={viewListRef} views={store.views} onEdit={handleEdit} />
                </ModalBody>
                <ModalFooter>
                    <Button
                        type='submit'
                        onClick={() => {
                            store.setViews?.((viewListRef.current as any).getViews())
                            setIsManageViewOpen(false)
                        }}
                    >
                        Apply
                    </Button>
                </ModalFooter>
            </Modal>
        </div>
    )
}

export default ConfigViews

const ConfigViewDropdown = React.forwardRef((props: any, ref) => {
    const overrides = {
        BaseButton: {
            style: {
                'paddingTop': '8px',
                'paddingBottom': '8px',
                'paddingLeft': '16px',
                'paddingRight': '16px',
                'width': '100%',
                'textAlign': 'left',
                'justifyContent': 'start',
                ':hover': {
                    backgroundColor: '#F0F4FF',
                },
            },
        },
    }

    const ListItem = ({
        data,
        index,
        style,
    }: {
        data: { props: OptionListProps }[]
        index: number
        style: React.CSSProperties
    }) => {
        // eslint-disable-next-line
        const { item, overrides, ...restChildProps } = data[index].props

        return (
            <StyledDropdownListItem
                className='text-ellipsis'
                title={item.label}
                style={{
                    boxSizing: 'border-box',
                    ...style,
                }}
                // eslint-disable-next-line
                {..._.omit(restChildProps, ['resetMenu', 'renderAll', 'renderHrefAsAnchor', 'getItemLabel'])}
                key={item.id}
            >
                {item.label}
            </StyledDropdownListItem>
        )
    }

    const children = React.Children.toArray(props.children)

    let items = null
    // @ts-ignore
    if (!children[0] || !children[0].props.item) {
        items = (
            <StyledEmptyState
                $style={{
                    boxShadow: 'none',
                }}
            />
        )
    } else {
        items = props.children.map((args: any, index: number) => (
            <ListItem key={index} index={index} data={props.children} style={args.style} />
        ))
    }

    return (
        <StyledList
            ref={ref}
            $style={{
                maxHeight: '60vh',
            }}
        >
            {items}
            <div
                style={{
                    borderTop: '1px solid #EEF1F6',
                    paddingTop: '8px',
                    paddingBottom: '8px',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'start',
                    gap: '8px',
                }}
            >
                {/* @ts-ignore */}
                <Button as='link' overrides={overrides} onClick={props.setIsAddViewOpen}>
                    Add View
                </Button>
                {/* @ts-ignore */}
                <Button as='link' overrides={overrides} onClick={props.setIsManageViewOpen}>
                    Manage View
                </Button>
            </div>
        </StyledList>
    )
})
