import { SwType } from '../model'
import { describe, expect } from '@jest/globals'

describe('SwType class', () => {
    it('should set name property correctly', () => {
        const name = 'test'
        const swType = new SwType(name)
        expect(swType.name).toEqual(name)
    })

    it('should return the input value for serialize method', () => {
        const swType = new SwType('test')
        const value = 'test value'
        expect(swType.serialize(value)).toEqual(value)
    })

    describe('decode_schema static method', () => {
        it("should return a new schema with value 'MAP' for input schema.type 'MAP'", () => {
            const schema = {
                value: [
                    {
                        key: {
                            type: 'INT64',
                            value: '1',
                        },
                        value: {
                            type: 'STRING',
                            value: 'bicm3c23utaujr5nvbgzwp3vmwlnhfgm3yzc2c3r',
                        },
                    },
                ],
                type: 'MAP',
            }
            const expectedSchema = { '1': 'bicm3c23utaujr5nvbgzwp3vmwlnhfgm3yzc2c3r' }
            expect(SwType.decode_schema(schema).value).toEqual(expectedSchema)
        })

        it("should return a new schema with value 'BYTES' for input schema.type 'BYTES'", () => {
            const schema = { type: 'BYTES', value: 'test value' }
            const expectedSchema = { type: 'BYTES', value: 'BYTES' }
            expect(SwType.decode_schema(schema)).toEqual(expectedSchema)
        })

        it("should return a new schema with value 'LIST' for input schema.type 'LIST'", () => {
            const schema = {
                type: 'LIST',
                value: [
                    {
                        type: 'INT64',
                        value: '28',
                    },
                    {
                        type: 'INT64',
                        value: '28',
                    },
                    {
                        type: 'INT64',
                        value: '1',
                    },
                ],
            }
            const expectedSchema = { type: 'LIST', value: ['28', '28', '1'] }
            expect(SwType.decode_schema(schema)).toEqual(expectedSchema)
        })

        it("should return a new schema with value 'TUPLE' for input schema.type 'TUPLE'", () => {
            const schema = {
                type: 'TUPLE',
                value: [
                    {
                        type: 'INT64',
                        value: '28',
                    },
                    {
                        type: 'INT64',
                        value: '28',
                    },
                    {
                        type: 'INT64',
                        value: '1',
                    },
                ],
            }
            const expectedSchema = { type: 'TUPLE', value: ['28', '28', '1'] }
            expect(SwType.decode_schema(schema)).toEqual(expectedSchema)
        })

        it("should return a new schema with value 'OBJECT' for input schema.type 'OBJECT'", () => {
            const schema = {
                type: 'OBJECT',
                value: {
                    mask_uri: {
                        type: 'STRING',
                        value: '',
                    },
                    shape: {
                        value: [
                            {
                                type: 'INT64',
                                value: '28',
                            },
                            {
                                type: 'INT64',
                                value: '28',
                            },
                            {
                                type: 'INT64',
                                value: '1',
                            },
                        ],
                        type: 'LIST',
                    },
                    link: {
                        value: {
                            owner: {
                                value: null,
                            },
                            extra_info: {
                                value: [
                                    {
                                        key: {
                                            type: 'STRING',
                                            value: 'bin_size',
                                        },
                                        value: {
                                            type: 'INT64',
                                            value: '1024',
                                        },
                                    },
                                    {
                                        key: {
                                            type: 'STRING',
                                            value: 'bin_offset',
                                        },
                                        value: {
                                            type: 'INT64',
                                            value: '9216',
                                        },
                                    },
                                ],
                                type: 'MAP',
                            },
                        },
                        type: 'OBJECT',
                        pythonType: 'starwhale.core.dataset.type.Link',
                    },
                    as_mask: {
                        type: 'BOOL',
                        value: 'false',
                    },
                },
            }
            const expectedSchema = {
                type: 'OBJECT',
                value: {
                    as_mask: 'false',
                    link: {
                        extra_info: {
                            bin_offset: '9216',
                            bin_size: '1024',
                        },
                        owner: null,
                    },
                    mask_uri: '',
                    shape: ['28', '28', '1'],
                },
            }
            expect(SwType.decode_schema(schema)).toEqual(expectedSchema)
        })

        it("should return the same schema for input schema.type other than 'MAP', 'BYTES', 'TUPLE', 'OBJECT'", () => {
            const schema = { type: 'INT', value: 'test value' }
            expect(SwType.decode_schema(schema)).toEqual(schema)
        })
    })

    it('should return the name property for toString method', () => {
        const name = 'test'
        const swType = new SwType(name)
        expect(swType.toString()).toEqual(name)
    })
})
