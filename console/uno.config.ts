// @ts-nocheck
import { defineConfig, presetAttributify, presetTypography, presetUno } from 'unocss'
import { presetScrollbar } from 'unocss-preset-scrollbar'

export default defineConfig({
    // ...UnoCSS options
    theme: {
        colors: {
            white: 'var(--novel-white)',
            stone: {
                50: 'var(--novel-stone-50)',
                100: 'var(--novel-stone-100)',
                200: 'var(--novel-stone-200)',
                300: 'var(--novel-stone-300)',
                400: 'var(--novel-stone-400)',
                500: 'var(--novel-stone-500)',
                600: 'var(--novel-stone-600)',
                700: 'var(--novel-stone-700)',
                800: 'var(--novel-stone-800)',
                900: 'var(--novel-stone-900)',
            },
        },
    },
    presets: [
        //presetAttributify(),
        presetUno(),
        presetScrollbar(),
        presetTypography({
            cssExtend: {},
        }),
    ],
    shortcuts: [
        ['wh-full', 'w-full h-full'],
        ['flex-col', 'flex flex-col'],
        ['f-c-c', 'flex justify-center items-center'],
        ['f-l-c', 'flex justify-start items-center'],
        [
            'icon-btn',
            'text-16 inline-block cursor-pointer select-none opacity-75 transition duration-200 ease-in-out hover:opacity-100 hover:text-primary !outline-none',
        ],
        ['content-full', 'flex flex-col overflow-hidden min-w-0 w-full flex-1'],
        ['content-full-scroll', 'flex flex-col overflow-scroll min-w-0 flex-1 '],
        // text
        [
            'button-link',
            'flex text-12px px-12px py-9px rounded-s decoration-none color-[rgb(2,16,43)] border-1 border-[#2b65d9] lh-none hover:color-[#5181e0] hover:border-[#5181e0]',
        ],
    ],
    rules: [
        ['card-shadow', { 'box-shadow': '0 2px 8px 0 rgba(0,0,0,0.20); ' }],
        ['card-shadow-md', { 'box-shadow': '0 4px 14px 0 rgba(0,0,0,0.30); ' }],
        ['card-shadow-md', { 'box-shadow': '0 4px 14px 0 rgba(0,0,0,0.30); ' }],
    ],
})
