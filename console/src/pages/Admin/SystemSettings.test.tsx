import * as React from 'react'
import { queryAllByText, waitFor, render } from '@testing-library/react'
import { routeRender } from '@/test/provider'
import Editor from '@monaco-editor/react'
import * as system from '@/domain/setting/services/system'
import SystemSettings from './SystemSettings'

jest.spyOn(system, 'fetchSystemSetting').mockImplementation(() =>
    Promise.resolve(`
        ---
        dockerSetting:
        registry: "abcd.com"
        `)
)

describe('SystemSettings', () => {
    describe('editor', () => {
        it('render', () => {
            const { container } = render(<Editor height='500px' width='960px' defaultLanguage='yaml' theme='vs-dark' />)
            expect(container).toMatchSnapshot()
        })
    })

    it('render', async () => {
        const { container } = routeRender(<SystemSettings />)
        expect(queryAllByText(container, 'Reset').length).toBe(1)
        expect(queryAllByText(container, 'Update').length).toBe(1)
    })

    it('render with fetchSystemSetting', async () => {
        routeRender(<SystemSettings />)
        // expect(container).toMatchSnapshot()
        // await expect(document.querySelector('.monaco-editor')).toBeTruthy()
        // await new Promise(process.nextTick)
        await expect(document.querySelector('.formItem')).toBeTruthy()
        // await waitFor(() => expect(queryByAttribute('class', document.body, 'monaco-editor')).toBeTruthy())
        // await waitFor(() => expect(document.querySelector('.monaco-editor')).toBeTruthy(), {
        //     timeout: 1000,
        // })
        await waitFor(() => expect(system.fetchSystemSetting).toHaveBeenCalled())
    })
})
