import { SwType } from '../model'
import { describe, expect, test } from '@jest/globals'

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
                value: {
                    '{type=INT64, value=1}': {
                        type: 'STRING',
                        value: 'bicm3c23utaujr5nvbgzwp3vmwlnhfgm3yzc2c3r',
                    },
                },
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

        it("should return a new schema with value 'TUPLE' for input schema.type 'TUPLE'", () => {
            const schema = { type: 'TUPLE', value: 'test value' }
            const expectedSchema = { type: 'TUPLE', value: 'TUPLE' }
            expect(SwType.decode_schema(schema)).toEqual(expectedSchema)
        })

        it("should return a new schema with value 'OBJECT' for input schema.type 'OBJECT'", () => {
            const schema = { type: 'OBJECT', value: 'test value' }
            const expectedSchema = { type: 'OBJECT', value: 'OBJECT' }
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
