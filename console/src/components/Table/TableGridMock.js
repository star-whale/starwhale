import React from 'react'
import { Column } from '@/components/BaseTable'
import { faker } from '@faker-js/faker'

export function randomColor() {
    return `hsl(${Math.floor(Math.random() * 360)}, 95%, 90%)`
}

const dataGenerator = () => ({
    id: faker.random.uuid(),
    name: faker.name.findName(),
    gender: faker.random.boolean() ? 'male' : 'female',
    score: {
        math: faker.random.number(70) + 30,
    },
    birthday: faker.date.between(1995, 2005),
    attachments: faker.random.number(5),
    description: faker.lorem.sentence(),
    email: faker.internet.email(),
    country: faker.address.country(),
    address: {
        street: faker.address.streetAddress(),
        city: faker.address.city(),
        zipCode: faker.address.zipCode(),
    },
})

const defaultSort = { key: 'name', order: 'asc' }

export function makeData(count) {
    let data = new Array(count)
        .fill(0)
        .map(dataGenerator)
        .sort((a, b) => (a.name > b.name ? 1 : -1))

    let columns = [
        {
            key: 'name',
            title: 'Name',
            dataKey: 'name',
            width: 150,
            resizable: true,
            sortable: true,
            frozen: Column.FrozenDirection.LEFT,
        },
        {
            key: 'score',
            title: 'Score',
            dataKey: 'score.math',
            width: 60,
            align: Column.Alignment.CENTER,
            sortable: false,
            cellRenderer: ({ cellData: score }) => <div score={score}>{score}</div>,
        },
        {
            key: 'gender',
            title: '♂♀',
            dataKey: 'gender',
            cellRenderer: ({ cellData: gender }) => <div gender={gender} />,
            width: 60,
            align: Column.Alignment.CENTER,
            sortable: true,
        },
        {
            key: 'birthday',
            title: 'Birthday',
            dataKey: 'birthday',
            dataGetter: ({ column, rowData }) => rowData[column.dataKey].toLocaleDateString(),
            width: 100,
            align: Column.Alignment.RIGHT,
            sortable: true,
        },
        {
            key: 'attachments',
            title: 'Attachments',
            dataKey: 'attachments',
            width: 60,
            align: Column.Alignment.CENTER,
            headerRenderer: () => <div>?</div>,
            cellRenderer: ({ cellData }) => <div>{cellData}</div>,
        },
        {
            key: 'description',
            title: 'Description',
            dataKey: 'description',
            width: 200,
            resizable: true,
            sortable: true,
            cellRenderer: ({ cellData }) => <div>{cellData}</div>,
        },
        {
            key: 'email',
            title: 'Email',
            dataKey: 'email',
            width: 200,
            resizable: true,
            sortable: true,
        },
        {
            key: 'country',
            title: 'Country',
            dataKey: 'country',
            width: 100,
            resizable: true,
            sortable: true,
        },
        {
            key: 'address',
            title: 'Address',
            dataKey: 'address.street',
            width: 200,
            resizable: true,
        },
        {
            key: 'action',
            width: 100,
            align: Column.Alignment.CENTER,
            frozen: Column.FrozenDirection.RIGHT,
            cellRenderer: ({ rowData }) => (
                <button
                    onClick={() => {
                        this.setState({
                            data: this.state.data.filter((x) => x.id !== rowData.id),
                        })
                    }}
                >
                    Remove
                </button>
            ),
        },
    ]
    return { columns, data, sortBy: defaultSort }
}

export const DataTypes = Object.freeze({
    NUMBER: 'number',
    TEXT: 'text',
    SELECT: 'select',
})
