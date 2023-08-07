import { defineConfig, presetAttributify, presetTypography, presetUno } from 'unocss'

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
        presetTypography({
            cssExtend: {},
        }),
    ],
})
