import update from 'immutability-helper'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { DndProvider } from 'react-dnd'
import { HTML5Backend } from 'react-dnd-html5-backend'
import { Card } from './DnDCard'
import { DnDDragLayer } from './DnDDragLayer'

const style = {
    // width: 400,
    width: '100%',
}

export interface IItem {
    id: string
    text: React.ReactElement
}

export interface IContainerState {
    cards: IItem[]
}

export interface IDnDContainerProps {
    data: IItem[]
    onOrderChange: (newOrder: string[], dragId: string) => void
}

export function DnDContainer(props: IDnDContainerProps) {
    const [cards, setCards] = useState(props.data ?? [])
    useEffect(() => {
        if (cards !== props.data) {
            setCards(props.data ?? [])
        }
    }, [props.data, cards])

    const moveCard = useCallback((dragIndex: number, hoverIndex: number) => {
        setCards((prevCards: IItem[]) => {
            const cardsNew = update(prevCards, {
                $splice: [
                    [dragIndex, 1],
                    [hoverIndex, 0, prevCards[dragIndex] as IItem],
                ],
            })

            // console.log('new cards', cards, dragIndex, hoverIndex, cardsNew)
            props.onOrderChange?.(
                cardsNew.map((card) => card.id),
                prevCards[dragIndex].id
            )
            return cardsNew
        })
    }, [])

    // const cards = useMemo(() => props.data ?? [], [props.data])
    // const moveCard = useCallback(
    //     (dragIndex: number, hoverIndex: number) => {
    //         const newCards = update(cards, {
    //             $splice: [
    //                 [dragIndex, 1],
    //                 [hoverIndex, 0, cards[dragIndex] as IItem],
    //             ],
    //         })
    //         props.onOrderChange?.(
    //             newCards.map((card) => card.id),
    //             cards[dragIndex].id
    //         )
    //     },
    //     [props, cards]
    // )

    const renderCard = useCallback(
        (card: { id: string; text: React.ReactElement }, index: number) => {
            return <Card key={card.id} index={index} id={card.id} text={card.text} moveCard={moveCard} />
        },
        [moveCard]
    )

    return (
        <div style={{ position: 'relative' }}>
            <DndProvider backend={HTML5Backend}>
                <DnDDragLayer snapToGrid />
                <div style={style}>{cards.map((card, i) => renderCard(card, i))}</div>
            </DndProvider>
        </div>
    )
}
