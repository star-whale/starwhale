import React from 'react'
import cn from 'classnames'
import { themedUseStyletron } from '../../theme/styletron'
import { HeaderContext } from './headers/header'
import CellPlacement from './cells/cell-placement'
import { VariableSizeGrid } from 'react-window'
import { ColumnT } from './types'
import { useIfChanged } from '@starwhale/core/utils'
import { filter } from 'rxjs/operators'
import _ from 'lodash'

function LoadingOrEmptyMessage(props: { children: React.ReactNode | (() => React.ReactNode) }) {
    const [css, theme] = themedUseStyletron()
    return (
        <div
            className={css({
                ...theme.typography.ParagraphSmall,
                color: theme.colors.contentPrimary,
                marginLeft: theme.sizing.scale500,
            })}
        >
            {typeof props.children === 'function' ? props.children() : String(props.children)}
        </div>
    )
}

const RENDERING = 0
const LOADING = 1
const EMPTY = 2

// @ts-ignore
const sum = (ns) => ns.reduce((s, n) => s + n, 0)
type InnerTableElementProps = {
    children: React.ReactNode | null
    style: React.CSSProperties
    data: any
}

// replaces the content of the virtualized window with contents. in this case,
// we are prepending a table header row before the table rows (children to the fn).
const InnerTableElement = React.forwardRef<HTMLDivElement, InnerTableElementProps>((props, ref) => {
    const [css] = themedUseStyletron()
    const ctx = React.useContext(HeaderContext)
    let viewState = RENDERING
    if (ctx.loading) {
        viewState = LOADING
    } else if (ctx.rows.length === 0) {
        viewState = EMPTY
    }
    const pinnedWidth = React.useMemo(
        () => sum(ctx.columns.map((v, index) => (v.pin === 'LEFT' ? ctx.widths[index] : 0))),
        [ctx.columns, ctx.widths]
    )

    const $childrenPinned = React.useMemo(() => {
        // @ts-ignore
        return Array.from(props.children ?? []).filter((child: any) => {
            const isPin = child.props.data.columns[child.props.columnIndex].pin === 'LEFT'
            return isPin
        })
    }, [props.children])

    const $children = React.useMemo(() => {
        return props.children
    }, [props.children])

    if (!ctx.widths.filter(Boolean).length) {
        return null
    }

    // useIfChanged(ctx)

    return (
        <>
            <div
                className={cn(
                    'table-inner-sticky',
                    css({
                        position: 'sticky',
                        width: 0,
                        height: 0,
                        left: 0,
                        zIndex: 100,
                        borderLeftWidth: '0',
                        borderRight: '1px solid #CFD7E6',
                        overflow: 'visible',
                        breakInside: 'avoid',
                    })
                )}
            >
                {viewState === RENDERING && $childrenPinned.length > 0 && (
                    <div
                        className='table-columns-pinned'
                        // @ts-ignore
                        style={{
                            ...props.style,
                            width: pinnedWidth,
                            position: 'relative',
                            overflow: 'hidden',
                        }}
                    >
                        {$childrenPinned}
                    </div>
                )}
            </div>

            <div
                // @ts-ignore
                ref={ref}
                className='table-inner'
                // @ts-ignore
                style={{
                    ...props.style,
                    minWidth: '100%',
                }}
            >
                {viewState === LOADING && <LoadingOrEmptyMessage>{ctx.loadingMessage}</LoadingOrEmptyMessage>}
                {viewState === EMPTY && <LoadingOrEmptyMessage>{ctx.emptyMessage}</LoadingOrEmptyMessage>}
                {viewState === RENDERING && $children}
            </div>
        </>
    )
})
InnerTableElement.displayName = 'InnerTableElement'

export default InnerTableElement
