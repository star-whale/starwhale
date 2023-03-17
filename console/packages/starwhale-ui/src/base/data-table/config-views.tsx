import React, { useMemo, useCallback, useRef, useEffect } from 'react'
import { StyledDropdownListItem } from 'baseui/select'
import Select from '@starwhale/ui/Select'
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
import { themedUseStyletron } from '../../theme/styletron'
import classNames from 'classnames'

type PropsT = {
    columns: ColumnT[]
    rows: any[]
    store: ITableState
    useStore: IStore
}

const ALLRUNS = 'all'

function ConfigViews(props: PropsT) {
    const [t] = useTranslation()

    const { store } = props
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
                    setSelectId(id)
                    store.onCurrentViewIdChange(id)
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
                    DialogContainer: {
                        style: {
                            height: '100vh',
                        },
                    },
                    Dialog: {
                        style: {
                            width: '700px',
                            display: 'flex',
                            flexDirection: 'column',
                            // minHeight: '640px',
                            maxHeight: 'calc(100% - 100px)',
                            overflow: 'hidden',
                        },
                    },
                }}
            >
                <ModalHeader>{!store.viewEditing?.id ? t('Add a New View') : t('Edit View')}</ModalHeader>
                <ModalBody
                    className='inherit-height'
                    style={{
                        flex: '1',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '20px',
                        overflow: 'auto',
                        paddingRight: '12px',
                    }}
                >
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

                            if (!newView.name) return toaster.negative('name required', { autoHideDuration: 2000 })

                            if (store.checkDuplicateViewName(newView.name, newView.id)) {
                                toaster.negative('View name already exists', { autoHideDuration: 2000 })
                                return
                            }

                            store.onViewUpdate(newView)
                            store.onShowViewModel(false, null)
                        }}
                    >
                        Save
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
                        Save
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
                    backgroundColor: '#EBF1FF',
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
        const [css] = themedUseStyletron()
        // eslint-disable-next-line
        const { item, overrides, ...restChildProps } = data[index].props

        return (
            <StyledDropdownListItem
                className={classNames(
                    'text-ellipsis',
                    css({
                        'boxSizing': 'border-box',
                        ':hover': {
                            backgroundColor: '#EBF1FF',
                        },
                    })
                )}
                style={style}
                title={item.label}
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
            ref={ref as any}
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
