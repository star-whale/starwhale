/* eslint-disable */
import * as React from 'react'
import { motion, useMotionValue, MotionValue, HTMLMotionProps, PanInfo } from 'framer-motion'
import { defaultEffects } from './defaultEffects'

/**
 * The size of the page component.
 */
export type Size = { width: number; height: number }

/**
 * A union of default page effects.
 */
export type DefaultEffect = 'coverflow' | 'pile' | 'cube' | 'wheel' | 'titles'

/** The type of the options passed into a PageEffect function. */
export type PageEffectInfo = {
    index: number
    pageCount: number
    direction: 'horizontal' | 'vertical'
    offset: number
    normalizedOffset: number
    size: Size
    gap: number
}

/**
 * A function that updates each page as the component changes pages.
 */
export type PageEffect = (info: PageEffectInfo) => Partial<HTMLMotionProps<'div'>>

/**
 * Props for `Page` component.
 */
export type PageProps = Partial<{
    /**
     * Distance (in pixels) between each page.
     */
    gap: number
    /**
     * The page index to display or turn to.
     */
    currentPage: number
    /**
     * Whether the page component should turn in the vertical or horizontal direction.
     */
    direction: 'horizontal' | 'vertical'
    /**
     * Where to align the pages along the horizontal axis.
     */
    originX: number
    /**
     * Where to align the pages along the vertical axis.
     */
    originY: number
    /**
     * A named default effect ("pile", "coverflow", "cube", or "wheel") or a PageEffect function.
     */
    effect: DefaultEffect | PageEffect
    /**
     * A motion value that is updated with the offset (x or y, depending on direction) of the component's content.
     */
    contentOffset: MotionValue<number>
    /**
     * A motion value that is updated with the component's current page as a floating point (e.g. 1.5 when half-way between the second and third page).
     */
    motionPage: MotionValue<number>
    /**
     * A motion value that is updated with the normalized progress of the component as it moves from start to end (e.g. 0 at the first page and 1 at the last).
     */
    progress: MotionValue<number>
    /**
     * An event that is triggered any time the user changes the current page through a swipe gesture.
     */
    onChangePage: (currentPage: number, previousPage: number) => void
}> &
    HTMLMotionProps<'div'>

/**
 * Page
 *
 * @public
 */
export const Page = React.forwardRef<HTMLDivElement, PageProps>((props, forwardedRef) => {
    const {
        currentPage = 0,
        gap = 0,
        direction = 'horizontal',
        transition = {
            type: 'spring',
            damping: 100,
            stiffness: 100,
            mass: 0.2,
        },
        originX = 0,
        originY = 0,
        dragElastic = 0.75,
        contentOffset,
        motionPage,
        progress,
        effect,
        onChangePage,
        children,
        ...rest
    } = props

    const isHorizontal = React.useMemo(() => direction === 'horizontal', [direction])

    const pageCount = React.useMemo(() => React.Children.count(children), [children])

    // ------------------------------ Refs -----------------------------------

    const rPrevious = React.useRef(-1)
    const rDragging = React.useRef(false)
    const rContainer = React.useRef<HTMLDivElement>(null)
    const rDraggable = React.useRef<HTMLDivElement>(null)

    // ----------------------------- State ----------------------------------

    const [originOffset, setOriginOffset] = React.useState({
        x: 0,
        y: 0,
    })

    const [size, setSize] = React.useState<Size>({
        width: 0,
        height: 0,
    })

    const [pages, setPages] = React.useState(children)

    const [current, setCurrent] = React.useState(currentPage)

    // ------------------------- Motion Values -------------------------------

    const wrapperOffset = useMotionValue(-((isHorizontal ? size.width : size.height) + gap) * currentPage)

    const draggableOffset = useMotionValue(0)

    const mvContentOffset = useMotionValue(wrapperOffset.get())

    // --------------------------- Callbacks ---------------------------------

    const runPageEffects = React.useCallback(
        function (offset: number, step: number, pageCount: number) {
            if (effect === undefined) return

            const origin = isHorizontal ? originOffset.x : originOffset.y

            const pages = React.Children.map(children, (child: React.ReactNode, index) => {
                const e = typeof effect === 'string' ? defaultEffects[effect] : effect

                const normalizedOffset = (offset - origin + index * step) / step

                const effectProps =
                    e({
                        index,
                        pageCount,
                        direction,
                        offset,
                        normalizedOffset,
                        size,
                        gap,
                    }) || {}

                if (React.isValidElement(child)) {
                    return React.cloneElement(child, {
                        ...child.props,
                        ...effectProps,
                        style: {
                            ...child.props.style,
                            ...effectProps.style,
                        },
                    })
                }
            })

            setPages(pages)
        },
        [children, direction, effect, gap, size]
    )

    // ----------------------------- Effects ---------------------------------

    // Set size from container offsets
    React.useLayoutEffect(() => {
        const draggable = rDraggable.current
        const container = rContainer.current

        if (draggable !== null && container !== null) {
            const contentSize = isHorizontal
                ? {
                      width: (draggable.offsetWidth + gap) / pageCount - gap,
                      height: draggable.offsetHeight,
                  }
                : {
                      width: draggable.offsetWidth,
                      height: (draggable.offsetHeight + gap) / pageCount - gap,
                  }

            setSize(contentSize)

            setOriginOffset({
                x: (container.offsetWidth - contentSize.width) * originX * -1,
                y: (container.offsetHeight - contentSize.height) * originY,
            })
        }
    }, [isHorizontal, originX, originY, pageCount, gap])

    // ---------- Current Page

    // Update current when currentPage changes
    React.useEffect(() => {
        if (currentPage <= pageCount - 1 && currentPage >= 0) {
            setCurrent((current) => {
                rPrevious.current = current
                return currentPage
            })
        }
    }, [currentPage, pageCount])

    // ---------- Content Offsets

    // Update mvContentOffset when dragging
    React.useEffect(() => {
        return draggableOffset.onChange((v) => {
            mvContentOffset.set(v + wrapperOffset.get())
        })
    }, [draggableOffset, mvContentOffset, wrapperOffset])

    // Update mvContentOffset when not dragging
    React.useEffect(() => {
        return wrapperOffset.onChange((v) => {
            if (rDragging.current) return

            mvContentOffset.set(v + draggableOffset.get())
        })
    }, [draggableOffset, mvContentOffset, wrapperOffset])

    // Update contentOffset when mvContentOffset changes
    React.useEffect(() => {
        if (contentOffset instanceof MotionValue) {
            return mvContentOffset.onChange((v) => {
                contentOffset.set(mvContentOffset.get())
            })
        }
    }, [contentOffset, mvContentOffset])

    // ---------- Progress / Motionpage / Page Effects

    // Update motion values when mvContentOffset changes
    React.useEffect(() => {
        return mvContentOffset.onChange((offset) => {
            const step = size.width + gap

            // Update motionPage
            if (motionPage instanceof MotionValue) {
                motionPage.set(-offset / step)
            }

            // Update progress
            if (progress instanceof MotionValue) {
                progress.set(-offset / step / (pageCount - 1))
            }

            // Update pages (page effects)
            runPageEffects(offset, step, pageCount)
        })
    }, [runPageEffects, motionPage, progress, mvContentOffset, direction, effect, gap, size, pageCount])

    // Update pages (page effects) on load
    React.useLayoutEffect(() => {
        const offset = mvContentOffset.get()
        const step = size.width + gap
        runPageEffects(offset, pageCount, step)
    }, [effect, gap, mvContentOffset, runPageEffects, pageCount, size.width])

    // --------------------------- Callbacks -------------------------------

    // Set dragging ref to true
    const handleDragStart = React.useCallback(() => {
        rDragging.current = true
    }, [])

    // Check whether drag caused a page change
    const handleDragEnd = React.useCallback(
        (event: MouseEvent | TouchEvent | PointerEvent, info: PanInfo) => {
            rDragging.current = false

            const { velocity, offset } = info
            let vel
            let off
            let dim

            if (isHorizontal) {
                off = offset.x
                vel = velocity.x
                dim = size.width
            } else {
                off = offset.y
                vel = velocity.y
                dim = size.height
            }

            const farEnough = Math.abs(off) > dim / 4
            const fastEnough = Math.abs(vel) > 75

            if (farEnough || fastEnough) {
                const delta = off > 0 ? -1 : 1
                const next = current + delta
                const max = pageCount - 1

                if (next !== current) {
                    rPrevious.current = current
                    setCurrent(Math.max(0, Math.min(max, next)))
                    onChangePage && onChangePage(next, rPrevious.current)
                }
            }
        },
        [isHorizontal, pageCount, size, current, setCurrent, onChangePage]
    )

    // ------------------------------ JSX ---------------------------------

    return (
        <motion.div
            ref={forwardedRef}
            {...rest}
            style={{
                transformStyle: 'preserve-3d',
                perspective: 1200,
                overflow: 'hidden',
                ...rest.style,
            }}
        >
            <motion.div
                ref={rContainer}
                initial={false}
                style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    justifyContent: 'flex-start',
                    flexDirection: isHorizontal ? 'row' : 'column',
                    transformStyle: 'preserve-3d',
                    height: '100%',
                    width: '100%',
                    ...(isHorizontal ? { x: wrapperOffset, y: 0 } : { x: 0, y: wrapperOffset }),
                }}
                animate={{
                    x: isHorizontal ? originOffset.x - (size.width + gap) * current : originOffset.x,
                    y: isHorizontal ? originOffset.y : originOffset.y - (size.height + gap) * current,
                }}
                transition={transition}
            >
                <motion.div
                    ref={rDraggable}
                    style={{
                        display: 'grid',
                        gridAutoColumns: 'auto',
                        gridAutoRows: 'auto',
                        gridAutoFlow: isHorizontal ? 'column' : 'row',
                        gap,
                        transformStyle: 'preserve-3d',
                        width: 'auto',
                        ...(isHorizontal ? { x: draggableOffset } : { y: draggableOffset }),
                    }}
                    drag={isHorizontal ? 'x' : 'y'}
                    dragElastic={dragElastic}
                    dragConstraints={{
                        left: 0,
                        right: 0,
                        top: 0,
                        bottom: 0,
                    }}
                    initial={false}
                    onDragStart={handleDragStart}
                    onDragEnd={handleDragEnd}
                >
                    {pages}
                </motion.div>
            </motion.div>
        </motion.div>
    )
})
