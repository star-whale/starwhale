import { useEffect, useRef } from 'react'
import { SubmitKey, useServingConfig } from '../store/config'

function useSubmitHandler() {
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const config = useServingConfig()
    const { submitKey } = config
    const isComposing = useRef(false)

    useEffect(() => {
        const onCompositionStart = () => {
            isComposing.current = true
        }
        const onCompositionEnd = () => {
            isComposing.current = false
        }

        window.addEventListener('compositionstart', onCompositionStart)
        window.addEventListener('compositionend', onCompositionEnd)

        return () => {
            window.removeEventListener('compositionstart', onCompositionStart)
            window.removeEventListener('compositionend', onCompositionEnd)
        }
    }, [])

    const shouldSubmit = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
        if (e.key !== 'Enter') return false
        if (e.key === 'Enter' && (e.nativeEvent.isComposing || isComposing.current)) return false
        return (
            (config.submitKey === SubmitKey.AltEnter && e.altKey) ||
            (config.submitKey === SubmitKey.CtrlEnter && e.ctrlKey) ||
            (config.submitKey === SubmitKey.ShiftEnter && e.shiftKey) ||
            (config.submitKey === SubmitKey.MetaEnter && e.metaKey) ||
            (config.submitKey === SubmitKey.Enter && !e.altKey && !e.ctrlKey && !e.shiftKey && !e.metaKey)
        )
    }

    return {
        submitKey,
        shouldSubmit,
    }
}

export default useSubmitHandler
