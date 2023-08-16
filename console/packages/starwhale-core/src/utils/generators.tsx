import { customAlphabet } from 'nanoid'

const ALPHANUMERIC = '1234567890abcdefghijklmnopqrstuvwxyz'
const nanoid = customAlphabet(ALPHANUMERIC, 10)
export const generateId = (prefix = ''): string => {
    return [prefix, nanoid()].filter(Boolean).join('-')
}
