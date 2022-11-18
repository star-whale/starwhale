import React, { useRef } from 'react'
import type { FC } from 'react'
import { useStyletron } from 'baseui'

export interface ICardProps {
    text: React.ReactElement
}

export const DnDCardDragPreview: FC<ICardProps> = ({ text }) => {
    const [css] = useStyletron()
    const ref = useRef<HTMLDivElement>(null)
    const backgroundColor = '#F0F4FF'

    return (
        <div
            ref={ref}
            className={css({
                height: '48px',
                lineheight: '16px',
                display: 'flex',
                alignItems: 'center',
                cursor: 'pointer',
                willchange: 'transform',
                flexWrap: 'nowrap',
                // justifyContent: 'space-between',
                backgroundColor,
                width: '100%',
            })}
        >
            {text}
        </div>
    )
}
