import React, { useState } from 'react'
// @ts-ignore
import { Resizable } from 'react-resizable'
import 'react-resizable/css/styles.css'

const RECT = { width: 400, height: 300 }

function CasecadeResizer({ children, defaultRect = RECT }) {
    const [rect, setRect] = useState(RECT)
    const [resizeRect, setResizeRect] = useState({
        start: false,
        ...RECT,
        left: 0,
        top: 0,
        clientX: 0,
        clientY: 0,
        offsetClientX: 0,
        offsetClientY: 0,
    })
    const previeRef = React.useRef<HTMLDivElement>(null)
    const wrapperRef = React.useRef<HTMLDivElement>(null)
    const memoChildren = React.useMemo(() => {
        return React.Children.map(children as any, (child: React.ReactElement) => {
            console.log(child.props)
            if (!child) return null

            return (
                <Resizable
                    width={rect.width}
                    height={rect.height}
                    axis='both'
                    onResizeStart={(e: any) => {
                        const parent = e.target.parentNode
                        const parentRect = parent.getBoundingClientRect()
                        setResizeRect({
                            start: true,
                            clientX: e.clientX,
                            clientY: e.clientY,
                            width: parentRect.width,
                            height: parentRect.height,
                            top: e.target.parentNode.offsetTop,
                            left: e.target.parentNode.offsetLeft,
                            offsetClientX: 0,
                            offsetClientY: 0,
                        })
                        previeRef.current?.focus()
                        e.stopPropagation()
                    }}
                    onResize={(e: any) => {
                        const wrapperWidth =
                            // @ts-ignore
                            wrapperRef.current?.getBoundingClientRect()?.width - padding * 2
                        if (resizeRect.width + e.clientX - resizeRect.clientX < defaultRect.width) return
                        if (resizeRect.height + e.clientY - resizeRect.clientY < defaultRect.height) return
                        if (resizeRect.width + e.clientX - resizeRect.clientX > wrapperWidth) return

                        setResizeRect({
                            ...resizeRect,
                            offsetClientX: e.clientX - resizeRect.clientX,
                            offsetClientY: e.clientY - resizeRect.clientY,
                        })
                    }}
                    onResizeStop={() => {
                        const rectTmp = {
                            width: resizeRect.width + resizeRect.offsetClientX,
                            height: resizeRect.height + resizeRect.offsetClientY,
                        }
                        setRect(rectTmp)
                        setResizeRect({
                            ...resizeRect,
                            start: false,
                        })
                    }}
                >
                    {child}
                </Resizable>
            )
        })
    }, [children, rect, resizeRect, defaultRect])

    return memoChildren
}

export { CasecadeResizer }

export default CasecadeResizer
