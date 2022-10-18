import '@testing-library/jest-dom'
import { isString } from 'lodash'
function applyErrorDetails(message: any) {
    return `Failing unit test due to unexpected console.error. Please resolve this in the relevant test.\n\n${
        message instanceof Error ? '${message}' : 'Error: '
    }${message}`
}

let error = console.error
console.error = function (message: any) {
    if (typeof message === 'string' && message.includes('Warning: Received `%s` for a non-boolean attribute `%s`'))
        return
    error.apply(console, arguments as any)
    throw new Error(applyErrorDetails(message))
}

function applyWarningDetails(message: any) {
    return `Failing unit test due to unexpected console.warn. Please mock and assert this in the relevant test.\n\nWarning: ${message}`
}

let warn = console.warn
console.warn = function (message: any) {
    if (typeof message === 'string' && message.includes('Warning: Received `%s` for a non-boolean attribute `%s`'))
        return

    warn.apply(console, arguments as any)
    throw new Error(applyWarningDetails(message))
}

// Object.defineProperty(window, 'matchMedia', {
//     writable: true,
//     value: jest.fn().mockImplementation((query) => ({
//         matches: false,
//         media: query,
//         onchange: null,
//         addListener: jest.fn(),
//         removeListener: jest.fn(),
//         addEventListener: jest.fn(),
//         removeEventListener: jest.fn(),
//         dispatchEvent: jest.fn(),
//     })),
// })

// All files must be modules when the '--isolatedModules' flag is provided.
export {}
