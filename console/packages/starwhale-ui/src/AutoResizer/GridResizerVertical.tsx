import IconFont from '../IconFont'
import classNames from 'classnames'
import React, { useCallback, useState } from 'react'
import { themedUseStyletron } from '../theme/styletron'

const RESIZEBAR_WIDTH = 60

const gridDefaultLayout = [
    // Top:
    `1fr ${RESIZEBAR_WIDTH - 8}px 0px`,
    // MIDDLE:
    `1fr ${RESIZEBAR_WIDTH}px 1fr`,
    // Bottom:
    `0px ${RESIZEBAR_WIDTH}px 1fr`,
]
export type GridResizerPropsT = {
    top: () => React.ReactNode
    bottom: () => React.ReactNode
    gridLayout?: string[]
    threshold?: number
    isResizeable?: boolean
    initGridMode?: number
    resizeTitle?: string
}

export function GridResizerVertical({
    top,
    bottom,
    gridLayout = gridDefaultLayout,
    threshold = 200,
    isResizeable = true,
    initGridMode = 0,
    resizeTitle = '',
}: GridResizerPropsT) {
    const [gridMode, setGridMode] = useState(initGridMode)
    const resizeRef = React.useRef<any>(null)
    const gridRef = React.useRef<HTMLDivElement>(null)
    const topRef = React.useRef<HTMLDivElement | null>(null)

    const grdiModeRef = React.useRef(initGridMode)
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
                <ResizeBar2
                    resizeTitle={resizeTitle}
                    mode={gridMode}
                    resizeRef={resizeRef}
                    // eslint-disable-next-line
                    onResizeStart={resizeStart}
                    onModeChange={setGridMode}
                />
            )}
            {isResizeable && bottom()}
        </div>
    )
}

export type ResizeBarPropsT = {
    resizeRef: React.RefObject<any>
    mode: number
    // eslint-disable-next-line
    onResizeStart?: () => void
    onModeChange: (mode: number) => void
    resizeTitle?: string
}

function ResizeBar2({ mode: gridMode = 0, resizeTitle = '', onModeChange, resizeRef }: ResizeBarPropsT) {
    const [css] = themedUseStyletron()

    return (
        <div
            ref={resizeRef}
            data-type='resize-bar-vertical'
            className={classNames(
                css({
                    marginTop: '8px',
                    paddingTop: '8px',
                    borderTop: '1px solid #e8e8e8',
                    width: '100%',
                    flexBasis: '100%',
                    zIndex: 20,
                    overflow: 'visible',
                    backgroundColor: '#fff',
                    position: 'relative',
                    display: 'flex',
                    justifyContent: 'space-between',
                    marginBottom: gridMode === 0 ? '0px' : '8px',
                })
            )}
            role='button'
            tabIndex={0}
            // onMouseDown={onResizeStart}
        >
            <div
                className={css({
                    color: 'rgba(2,16,43,0.60)',
                    fontSize: '14px',
                    fontWeight: 600,
                    lineHeight: '36px',
                })}
            >
                {resizeTitle}
            </div>
            <div
                className={css({
                    border: '1px solid #CFD7E6',
                    borderRadius: '4px',
                    alignSelf: 'flex-end',
                    padding: '6px 0 ',
                })}
            >
                {['layout-1', 'layout-2', 'layout-3'].map((icon, index) => {
                    return (
                        <i
                            key={icon}
                            role='button'
                            tabIndex={index}
                            onClick={() => onModeChange(index)}
                            style={{
                                display: 'inline-flex',
                                width: '40px',
                                height: '14px',
                                justifyContent: 'center',
                                alignItems: 'center',
                                borderLeft: index === 1 ? '1px solid #CFD7E6' : 'none',
                                borderRight: index === 1 ? '1px solid #CFD7E6' : 'none',
                            }}
                        >
                            <IconFont
                                type={icon as any}
                                size={14}
                                style={{
                                    color: gridMode !== index ? 'rgba(2,16,43,0.40)' : '#2B65D9',
                                    marginBottom: '2px',
                                }}
                            />
                        </i>
                    )
                })}
            </div>
        </div>
    )
}
