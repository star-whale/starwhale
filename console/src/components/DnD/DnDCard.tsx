import React, { useRef } from 'react'
import type { Identifier, XYCoord } from 'dnd-core'
import type { FC } from 'react'
import { useDrag, useDrop } from 'react-dnd'
import { useStyletron } from 'baseui'

export const ItemTypes = {
    CARD: 'card',
}

export interface ICardProps {
    id: any
    text: React.ReactElement
    index: number
    moveCard: (dragIndex: number, hoverIndex: number) => void
}

interface IDragItem {
    index: number
    id: string
    type: string
}

export const Card: FC<ICardProps> = ({ id, text, index, moveCard }) => {
    const [css] = useStyletron()
    const ref = useRef<HTMLDivElement>(null)
    const [{ handlerId }, drop] = useDrop<IDragItem, void, { handlerId: Identifier | null }>({
        accept: ItemTypes.CARD,
        collect(monitor) {
            return {
                handlerId: monitor.getHandlerId(),
            }
        },
        hover(item: IDragItem, monitor) {
            if (!ref.current) {
                return
            }
            const dragIndex = item.index
            const hoverIndex = index

            // Don't replace items with themselves
            if (dragIndex === hoverIndex) {
                return
            }

            // Determine rectangle on screen
            const hoverBoundingRect = ref.current?.getBoundingClientRect()

            // Get vertical middle
            const hoverMiddleY = (hoverBoundingRect.bottom - hoverBoundingRect.top) / 2

            // Determine mouse position
            const clientOffset = monitor.getClientOffset()

            // Get pixels to the top
            const hoverClientY = (clientOffset as XYCoord).y - hoverBoundingRect.top

            // Only perform the move when the mouse has crossed half of the items height
            // When dragging downwards, only move when the cursor is below 50%
            // When dragging upwards, only move when the cursor is above 50%

            // Dragging downwards
            if (dragIndex < hoverIndex && hoverClientY < hoverMiddleY) {
                return
            }

            // Dragging upwards
            if (dragIndex > hoverIndex && hoverClientY > hoverMiddleY) {
                return
            }

            // Time to actually perform the action
            moveCard(dragIndex, hoverIndex)

            // Note: we're mutating the monitor item here!
            // Generally it's better to avoid mutations,
            // but it's good here for the sake of performance
            // to avoid expensive index searches.

            /* eslint-disable no-param-reassign */
            item.index = hoverIndex
        },
    })

    const [{ isDragging }, drag] = useDrag({
        type: ItemTypes.CARD,
        item: () => {
            return { id, index, text }
        },
        collect: (monitor: any) => ({
            isDragging: monitor.isDragging(),
        }),
    })

    const opacity = isDragging ? 0.5 : 1
    const backgroundColor = isDragging ? '#F0F4FF' : '#FFF'
    drag(drop(ref))

    return (
        <div
            ref={ref}
            className={css({
                ':hover': {
                    // backgroundColor: isDragging ? '#fff' : '#F0F4FF',
                },
                opacity,
                backgroundColor,
            })}
            data-handler-id={handlerId}
        >
            {text}
        </div>
    )
}
