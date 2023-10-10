export function isDebug() {
    // @ts-ignore
    return window?.localStorage?.getItem('debug') === 'true' || import.meta.env.DEV
}
