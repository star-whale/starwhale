import * as React from 'react'
import { TableBuilder, TableBuilderColumn } from 'baseui/table-semantic'
import { StyledLink } from 'baseui/link'
import useTranslation from '@/hooks/useTranslation'
import { useDeepEffect } from '@/hooks/useDeepEffects'
import { useStyletron } from 'baseui'
import { ConfigT } from './types'
import Toggle from '../Select/Toggle'

type ViewListPropsT = {
    views: ConfigT[]
    onEdit: (view: ConfigT, index: number) => void
}
function ViewList(props: ViewListPropsT, ref: React.Ref<any>) {
    const [t] = useTranslation()
    const [, theme] = useStyletron()
    const [views, setViews] = React.useState(
        props.views.map((v) => ({
            ...v,
        }))
    )
    useDeepEffect(() => {
        setViews(
            props.views.map((v) => ({
                ...v,
            }))
        )
    }, [props.views])

    const handleDefault = React.useCallback(
        (view, rowIndex, value) => {
            const $views = views.map((v) => ({
                ...v,
                def: false,
            }))
            $views[rowIndex] = {
                ...view,
                def: value,
            }
            setViews([...$views])
        },
        [views, setViews]
    )
    const handleShow = React.useCallback(
        (view, rowIndex, value) => {
            const $views = views.map((v) => v)
            $views[rowIndex] = {
                ...view,
                isShow: value,
            }
            setViews([...$views])
        },
        [views, setViews]
    )
    const handleDelete = React.useCallback(
        (view, rowIndex) => {
            const isDeleteDefault = view.def
            views.splice(rowIndex, 1)
            if (isDeleteDefault)
                views[views.length - 1] = {
                    ...views[views.length - 1],
                    def: true,
                }
            setViews([...views])
        },
        [views, setViews]
    )

    React.useImperativeHandle(
        ref,
        () => ({
            getViews: () => views,
            getViewIndex: () => views,
        }),
        [views]
    )

    return (
        <TableBuilder
            data={views}
            overrides={{
                Root: { style: { maxHeight: '50vh' } },
                TableBodyRow: {
                    style: {
                        cursor: 'pointer',
                        borderRadius: theme.borders.radius100,
                    },
                },
                TableHeadCell: {
                    style: {
                        backgroundColor: 'var(--color-brandTableHeaderBackground)',
                        fontWeight: 'bold',
                        borderBottomWidth: 0,
                        fontSize: 14,
                        lineHeight: '16px',
                        paddingTop: '15px',
                        paddingBottom: '15px',
                        paddingLeft: '20px',
                        paddingRight: '20px',
                    },
                },
                TableHeadRow: {
                    style: {
                        borderRadius: '4px',
                    },
                },
                TableBodyCell: {
                    style: {
                        paddingTop: 0,
                        paddingBottom: 0,
                        paddingLeft: '20px',
                        paddingRight: '20px',
                        lineHeight: '44px',
                    },
                },
                // ...overrides,
            }}
        >
            <TableBuilderColumn
                header='Default'
                overrides={{
                    TableBodyCell: {
                        style: {
                            verticalAlign: 'middle',
                        },
                    },
                }}
            >
                {(row, rowIndex) => <Toggle value={row.def} onChange={(v) => handleDefault(row, rowIndex, v)} />}
            </TableBuilderColumn>
            <TableBuilderColumn
                header='Visibility'
                overrides={{
                    TableBodyCell: {
                        style: {
                            verticalAlign: 'middle',
                        },
                    },
                }}
            >
                {(row, rowIndex) => <Toggle value={row.isShow} onChange={(v) => handleShow(row, rowIndex, v)} />}
            </TableBuilderColumn>
            <TableBuilderColumn header='Views'>{(row) => row.name}</TableBuilderColumn>
            <TableBuilderColumn header=''>
                {(row, rowIndex) => (
                    <>
                        <StyledLink onClick={() => props.onEdit?.(row, rowIndex as number)}>{t('Edit')}</StyledLink>
                        &nbsp;&nbsp;
                        <StyledLink onClick={() => handleDelete(row, rowIndex)}>{t('Delete')}</StyledLink>
                    </>
                )}
            </TableBuilderColumn>
        </TableBuilder>
    )
}

export default React.forwardRef(ViewList)
