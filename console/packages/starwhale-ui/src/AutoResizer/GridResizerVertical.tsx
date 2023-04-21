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
    top: () => React.ReactNode
    bottom: () => React.ReactNode
    gridLayout?: string[]
    threshold?: number
    isResizeable?: boolean
}

export function GridResizerVertical({
    top,
    bottom,
    gridLayout = gridDefaultLayout,
    threshold = 200,
    isResizeable = true,
}: GridResizerPropsT) {
    const [gridMode, setGridMode] = useState(1)
    const resizeRef = React.useRef<any>(null)
    const gridRef = React.useRef<HTMLDivElement>(null)
    const topRef = React.useRef<HTMLDivElement | null>(null)

    const grdiModeRef = React.useRef(1)
    const resize = useCallback(
        (e: MouseEvent) => {
            window.requestAnimationFrame(() => {
                if (resizeRef.current && topRef.current) {
                    const offset = resizeRef.current.getBoundingClientRect().top - e.clientY
                    // topRef.current!.style.width = `${topRef.current?.getBoundingClientRect().width - offset}px`
                    // topRef.current!.style.flexBasis = `${topRef.current?.getBoundingClientRect().width - offset}px`
                    // console.log('resize', topRef.current?.getBoundingClientRect(), e.clientX, offset)
                    const newHeight = topRef.current?.getBoundingClientRect().height - offset
                    // eslint-disable-next-line
                    if (newHeight + threshold > gridRef.current!.getBoundingClientRect().height) {
                        grdiModeRef.current = 2
                        setGridMode(2)
                    } else if (newHeight < threshold) {
                        grdiModeRef.current = 0
                        setGridMode(0)
                    } else if (grdiModeRef.current === 1) {
                        // eslint-disable-next-line
                        gridRef.current!.style.gridTemplateRows = `${Math.max(
                            newHeight,
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
            ref={gridRef}
            style={{
                display: 'grid',
                gridTemplateRows: isResizeable ? gridLayout[gridMode] : '1fr',
                overflow: 'hidden',
                width: '100%',
                height: '100%',
                flex: 1,
            }}
        >
            <div
                ref={topRef}
                style={{
                    display: 'flex',
                    overflow: 'hidden',
                    width: '100%',
                    flex: 1,
                }}
            >
                {top()}
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
            {isResizeable && bottom()}
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
                'resize-bar-vertical',
                css({
                    width: '100%',
                    flexBasis: '100%',
                    cursor: 'row-resize',
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
            <i
                role='button'
                tabIndex={0}
                className='resize-top resize-top--hover'
                onClick={() => onModeChange(1)}
                style={{
                    transform: 'rotate(90deg)',
                }}
            >
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
            <i
                role='button'
                tabIndex={0}
                className='resize-bottom resize-bottom--hover'
                onClick={() => onModeChange(-1)}
                style={{
                    transform: 'rotate(90deg)',
                }}
            >
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
