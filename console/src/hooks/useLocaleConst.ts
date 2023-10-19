import { localeConst } from '@/consts'
import useTranslation from './useTranslation'

function useLocaleConst(str: string) {
    const [, i18n] = useTranslation()

    return localeConst[i18n.language]?.[str]
}

export { useLocaleConst }
export default useLocaleConst
