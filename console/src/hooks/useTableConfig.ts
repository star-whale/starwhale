import { useLocalStorage } from 'react-use'

export const useTableConfig = (keys: Array<any>, value: Record<string, any>) => {
    const [config, setConfig] = useLocalStorage(['table', ...keys].join('/'), value)

    return {
        config: config ?? {},
        setConfig,
    }
}

export const useTableViewConfig = <T>(keys: Array<any>, value: T) => {
    const [config, setConfig] = useLocalStorage(['table', ...keys].join('/'), value)

    return {
        config,
        setConfig,
    }
}
