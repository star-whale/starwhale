import React, { useMemo, useCallback, useRef, useEffect } from 'react'
import { StyledDropdownListItem } from 'baseui/select'
import Select from '@starwhale/ui/Select'
import { StyledList, StyledEmptyState, OptionListProps } from 'baseui/menu'
import { Modal, ModalBody, ModalFooter, ModalHeader } from 'baseui/modal'
import useTranslation from '@/hooks/useTranslation'
import _ from 'lodash'
import { toaster } from 'baseui/toast'
import ViewList from './ConfigViewList'
import ViewsEdit from './ConfigViewEdit'
import { ITableState } from '../../store'
import classNames from 'classnames'
import { useStore, useStoreApi } from '../../hooks/useStore'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import Button from '../../../Button'
import shallow from 'zustand/shallow'
import useGrid from '../../hooks/useGrid'

const ALLRUNS = 'all'

const selector = (s: ITableState) => ({
    currentView: s.currentView,
    views: s.views,
    viewEditing: s.viewEditing,
    viewModelShow: s.viewModelShow,
})

function ConfigViews() {
    const store = useStoreApi()
    const { currentView, views, viewModelShow, viewEditing } = useStore(selector, shallow)
    const { onShowViewModel, onCurrentViewIdChange, checkDuplicateViewName, onViewUpdate, setViews, columnTypes } =
        store.getState()
    const [t] = useTranslation()
    const [isManageViewOpen, setIsManageViewOpen] = React.useState(false)
    const [selectId, setSelectId] = React.useState(currentView?.id ?? '')
    const { originalColumns } = useGrid()

    useEffect(() => {
        if (currentView) {
            setSelectId(currentView?.id ?? '')
        }
    }, [currentView])

    const $options: any = useMemo(() => {
        return [
            ...views
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
    }, [views, t])

    const viewListRef = useRef(null)
    const viewRef = useRef(null)

    const handleEdit = useCallback(
        (view) => {
            onShowViewModel(true, view)
        },
        [onShowViewModel]
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
                            setIsAddViewOpen: () => onShowViewModel(true, null),
                            setIsManageViewOpen: () => setIsManageViewOpen(true),
                        },
                        // eslint-disable-next-line @typescript-eslint/no-use-before-define
                        component: ConfigViewDropdown,
                    },
                }}
                onChange={(params) => {
                    const id = params.option?.id as string
                    setSelectId(id)
                    onCurrentViewIdChange(id)
                }}
                value={selectId ? [{ id: selectId }] : []}
            />
            <Modal
                isOpen={viewModelShow}
                onClose={() => onShowViewModel(false, null)}
                closeable
                animate
                autoFocus
                returnFocus={false}
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
                <ModalHeader>{!viewEditing?.id ? t('Add a New View') : t('Edit View')}</ModalHeader>
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
                        columns={originalColumns as any}
                        view={viewEditing}
                        columnTypes={columnTypes}
                    />
                </ModalBody>
                <ModalFooter>
                    <Button
                        type='submit'
                        onClick={(event) => {
                            event.preventDefault()
                            const newView = (viewRef.current as any).getView()

                            if (!newView.name) return toaster.negative('name required', { autoHideDuration: 2000 })

                            if (checkDuplicateViewName(newView.name, newView.id)) {
                                return toaster.negative(t('table.view.name.exsts'), { autoHideDuration: 2000 })
                            }

                            onViewUpdate(newView)
                            onShowViewModel(false, null)
                            return false
                        }}
                    >
                        {t('save')}
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
                    <ViewList ref={viewListRef} views={views} onEdit={handleEdit} />
                </ModalBody>
                <ModalFooter>
                    <Button
                        type='submit'
                        onClick={() => {
                            setViews?.((viewListRef.current as any).getViews())
                            setIsManageViewOpen(false)
                        }}
                    >
                        {t('save')}
                    </Button>
                </ModalFooter>
            </Modal>
        </div>
    )
}

export default ConfigViews

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
    const [t] = useTranslation()

    const children = React.Children.toArray(props.children)
    let items = null
    // @ts-ignore
    if (!children[0] || !children[0].props.item) {
        // @ts-ignore
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
                    {t('table.view.add')}
                </Button>
                {/* @ts-ignore */}
                <Button as='link' overrides={overrides} onClick={props.setIsManageViewOpen}>
                    {t('table.view.manage')}
                </Button>
            </div>
        </StyledList>
    )
})
