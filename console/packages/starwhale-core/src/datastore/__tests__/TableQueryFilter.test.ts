import { DataTypes, OPERATOR } from '../constants'
import TableQueryFilter from '../schemas/TableQueryFilter'
import { describe, expect } from '@jest/globals'

describe('TableQueryFilter class', () => {
    it('not in with multi value', () => {
        const expectedSchema = {
            filter: {
                operator: 'NOT',
                operands: [
                    {
                        filter: {
                            operator: 'OR',
                            operands: [
                                {
                                    filter: {
                                        operator: 'EQUAL',
                                        operands: [{ columnName: 'sys/id' }, { stringValue: '45' }],
                                    },
                                },
                                {
                                    filter: {
                                        operator: 'EQUAL',
                                        operands: [{ columnName: 'sys/id' }, { stringValue: '46' }],
                                    },
                                },
                            ],
                        },
                    },
                ],
            },
        }
        expect(
            TableQueryFilter.fromUI({
                operator: OPERATOR.NOT_IN,
                columnName: 'sys/id',
                value: '45,46',
                type: DataTypes.STRING,
            })?.toJSON()
        ).toEqual(expectedSchema)
    })

    it('not in with single value', () => {
        const expectedSchema = {
            filter: {
                operator: 'NOT',
                operands: [
                    {
                        filter: {
                            operator: 'EQUAL',
                            operands: [{ columnName: 'sys/id' }, { stringValue: '45' }],
                        },
                    },
                ],
            },
        }
        expect(
            TableQueryFilter.fromUI({
                operator: OPERATOR.NOT_IN,
                columnName: 'sys/id',
                value: '45',
                type: DataTypes.STRING,
            })?.toJSON()
        ).toEqual(expectedSchema)
    })

    it('in with multi value', () => {
        const expectedSchema = {
            filter: {
                operator: 'OR',
                operands: [
                    {
                        filter: {
                            operator: 'EQUAL',
                            operands: [{ columnName: 'sys/id' }, { stringValue: '45' }],
                        },
                    },
                    {
                        filter: {
                            operator: 'EQUAL',
                            operands: [{ columnName: 'sys/id' }, { stringValue: '46' }],
                        },
                    },
                ],
            },
        }
        expect(
            TableQueryFilter.fromUI({
                operator: OPERATOR.IN,
                columnName: 'sys/id',
                value: '45,46',
                type: DataTypes.STRING,
            })?.toJSON()
        ).toEqual(expectedSchema)
    })

    it('in with single value', () => {
        const expectedSchema = {
            filter: {
                operator: 'EQUAL',
                operands: [{ columnName: 'sys/id' }, { stringValue: '45' }],
            },
        }
        expect(
            TableQueryFilter.fromUI({
                operator: OPERATOR.IN,
                columnName: 'sys/id',
                value: '45',
                type: DataTypes.STRING,
            })?.toJSON()
        ).toEqual(expectedSchema)
    })

    it('and', () => {
        const expectedSchema = {
            operator: OPERATOR.AND,
            operands: [
                {
                    filter: {
                        operator: 'EQUAL',
                        operands: [{ columnName: 'sys/id' }, { stringValue: '45' }],
                    },
                },
            ],
        }
        expect(
            TableQueryFilter.AND(
                TableQueryFilter.fromUI({
                    operator: OPERATOR.IN,
                    columnName: 'sys/id',
                    value: '45',
                    type: DataTypes.STRING,
                })?.toJSON()
            )
        ).toEqual(expectedSchema)
    })

    it('greater', () => {
        const expectedSchema = {
            filter: {
                operator: 'GREATER',
                operands: [{ columnName: 'sys/id' }, { stringValue: '45' }],
            },
        }
        expect(
            TableQueryFilter.fromUI({
                operator: OPERATOR.GREATER,
                columnName: 'sys/id',
                value: '45',
                type: DataTypes.STRING,
            })?.toJSON()
        ).toEqual(expectedSchema)
    })
})
