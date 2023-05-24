import IconFont from '../IconFont'
import classNames from 'classnames'
import React, { useCallback, useState } from 'react'
import { themedUseStyletron } from '../theme/styletron'

const gridDefaultLayout = [
    // RIGHT:
    '0px 40px 1fr',
    // MIDDLE:
    '1fr 40px 1fr',
    // LEFT:
    '1fr 40px 0px',
]
const RESIZEBAR_WIDTH = 40
export type GridResizerPropsT = {
    left: () => React.ReactNode
    right: () => React.ReactNode
    gridLayout?: string[]
    threshold?: number
    isResizeable?: boolean
}

export function GridResizer({
    left,
    right,
    gridLayout = gridDefaultLayout,
    threshold = 200,
    isResizeable = true,
}: GridResizerPropsT) {
    const [gridMode, setGridMode] = useState(1)
    const resizeRef = React.useRef<any>(null)
    const gridRef = React.useRef<HTMLDivElement>(null)
    const leftRef = React.useRef<HTMLDivElement | null>(null)

    const grdiModeRef = React.useRef(1)
    const resize = useCallback(
        (e: MouseEvent) => {
            window.requestAnimationFrame(() => {
                if (resizeRef.current && leftRef.current) {
                    const offset = resizeRef.current.getBoundingClientRect().left - e.clientX
                    // leftRef.current!.style.width = `${leftRef.current?.getBoundingClientRect().width - offset}px`
                    // leftRef.current!.style.flexBasis = `${leftRef.current?.getBoundingClientRect().width - offset}px`
                    // console.log('resize', leftRef.current?.getBoundingClientRect(), e.clientX, offset)
                    const newWidth = leftRef.current?.getBoundingClientRect().width - offset
                    // eslint-disable-next-line
                    if (newWidth + threshold > gridRef.current!.getBoundingClientRect().width) {
                        grdiModeRef.current = 2
                        setGridMode(2)
                    } else if (newWidth < threshold) {
                        grdiModeRef.current = 0
                        setGridMode(0)
                    } else if (grdiModeRef.current === 1) {
                        // eslint-disable-next-line
                        gridRef.current!.style.gridTemplateColumns = `${Math.max(
                            newWidth,
                            threshold
                        )}px ${RESIZEBAR_WIDTH}px minmax(${threshold}px, 1fr)`
                    }
                }
            })
        },
        [grdiModeRef, setGridMode, threshold]
    )
    const resizeEnd = () => {
        document.body.style.userSelect = 'unset'
        document.body.style.cursor = 'unset'
        document.removeEventListener('mouseup', resizeEnd)
        document.removeEventListener('mousemove', resize)
    }
    const resizeStart = () => {
        if (gridMode !== 1) return
        grdiModeRef.current = 1
        document.body.style.userSelect = 'none'
        document.body.style.cursor = 'col-resize'
        document.addEventListener('mouseup', resizeEnd)
        document.addEventListener('mousemove', resize)
    }

    React.useEffect(() => {
        return resizeEnd
    })

    const handleResize = useCallback(
        (dir) => {
            let next = Math.min(gridLayout.length - 1, gridMode + dir)
            next = Math.max(0, next)
            grdiModeRef.current = next
            setGridMode(next)
        },
        [gridMode, setGridMode, grdiModeRef, gridLayout.length]
    )
    return (
        <div
            data-type='grid-resizer'
            ref={gridRef}
            style={{
                display: 'grid',
                gridTemplateColumns: isResizeable ? gridLayout[gridMode] : '1fr',
                overflow: 'hidden',
                width: '100%',
                height: '100%',
                flex: 1,
            }}
        >
            <div
                data-type='grid-resizer-left'
                ref={leftRef}
                style={{
                    display: 'flex',
                    overflow: 'hidden',
                    width: '100%',
                    flex: 1,
                }}
            >
                {left()}
            </div>
            {isResizeable && (
                // eslint-disable-next-line  @typescript-eslint/no-use-before-define
                <ResizeBar
                    mode={gridMode}
                    resizeRef={resizeRef}
                    onResizeStart={resizeStart}
                    onModeChange={handleResize}
                />
            )}
            {isResizeable && (
                <div
                    data-type='grid-resizer-right'
                    style={{
                        display: 'flex',
                        flexDirection: 'column',
                        overflow: 'hidden',
                        width: '100%',
                        flex: 1,
                    }}
                >
                    {gridMode !== 2 && right()}
                </div>
            )}
        </div>
    )
}

export type ResizeBarPropsT = {
    resizeRef: React.RefObject<any>
    mode: number
    onResizeStart: () => void
    onModeChange: (mode: number) => void
}

function ResizeBar({ mode: gridMode = 2, onResizeStart, onModeChange, resizeRef }: ResizeBarPropsT) {
    const [css] = themedUseStyletron()

    return (
        <div
            ref={resizeRef}
            className={classNames(
                'resize-bar',
                css({
                    width: `${RESIZEBAR_WIDTH}px`,
                    flexBasis: `${RESIZEBAR_WIDTH}px`,
                    cursor: 'col-resize',
                    paddingTop: '25px',
                    zIndex: 20,
                    overflow: 'visible',
                    backgroundColor: '#fff',
                    position: 'relative',
                    right: gridMode === 2 ? '0px' : undefined,
                    left: gridMode === 0 ? '0px' : undefined,
                })
            )}
            role='button'
            tabIndex={0}
            onMouseDown={onResizeStart}
        >
            <i role='button' tabIndex={0} className='resize-left resize-left--hover' onClick={() => onModeChange(1)}>
                <IconFont
                    type='fold2'
                    size={12}
                    style={{
                        color: gridMode !== 2 ? undefined : '#ccc',
                        transform: 'rotate(-90deg) translateY(-2px)',
                        marginBottom: '2px',
                    }}
                />
            </i>
            <i role='button' tabIndex={0} className='resize-right resize-right--hover' onClick={() => onModeChange(-1)}>
                <IconFont
                    type='unfold2'
                    size={12}
                    style={{
                        color: gridMode !== 0 ? undefined : '#ccc',
                        transform: 'rotate(-90deg) translateY(2px)',
                    }}
                />
            </i>
        </div>
    )
}
