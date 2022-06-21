import update from 'immutability-helper'
import React, { useEffect } from 'react'
import { useCallback, useState } from 'react'
import { DndProvider } from 'react-dnd'
import { HTML5Backend } from 'react-dnd-html5-backend'
import { Card } from './DnDCard'
import { DnDDragLayer } from './DnDDragLayer'

const style = {
    // width: 400,
}

export interface Item {
    id: string
    text: React.ReactElement
}

export interface ContainerState {
    cards: Item[]
}

export function DnDContainer(props: { data: Item[]; onOrderChange: (newOrder: string[]) => void }) {
    {
        const [cards, setCards] = useState(props.data ?? [])
        useEffect(() => {
            if (cards != props.data) {
                setCards(props.data ?? [])
            }
        }, [props.data])

        // const moveCard = useCallback((dragIndex: number, hoverIndex: number) => {
        //     setCards((prevCards: Item[]) => {
        //         const cardsNew = update(prevCards, {
        //             $splice: [
        //                 [dragIndex, 1],
        //                 [hoverIndex, 0, prevCards[dragIndex] as Item],
        //             ],
        //         })

        //         console.log('new cards', cards, dragIndex, hoverIndex, cardsNew)
        //         props.onOrderChange?.(cardsNew.map((card) => card.id))
        //         return cardsNew
        //     })
        // }, [])

        // const cards = props.data ?? []
        // console.log('dndContainer: useEffect', cards)

        const moveCard = useCallback(
            (dragIndex: number, hoverIndex: number) => {
                const newCards = update(cards, {
                    $splice: [
                        [dragIndex, 1],
                        [hoverIndex, 0, cards[dragIndex] as Item],
                    ],
                })
                props.onOrderChange?.(newCards.map((card) => card.id))
            },
            [props, cards]
        )

        const renderCard = useCallback(
            (card: { id: string; text: React.ReactElement }, index: number) => {
                return <Card key={card.id} index={index} id={card.id} text={card.text} moveCard={moveCard} />
            },
            [moveCard]
        )

        return (
            <div style={{ position: 'relative' }}>
                <DndProvider backend={HTML5Backend}>
                    <DnDDragLayer snapToGrid={true} />
                    <div style={style}>{cards.map((card, i) => renderCard(card, i))}</div>
                </DndProvider>
            </div>
        )
    }
}
