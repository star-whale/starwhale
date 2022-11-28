// based on https://github.com/WICG/focus-visible/blob/v4.1.5/src/focus-visible.js
import * as React from 'react'

let hadKeyboardEvent = true
let hadFocusVisibleRecently = false
let hadFocusVisibleRecentlyTimeout: undefined | number

const inputTypesWhitelist: Record<string, boolean> = {
    'text': true,
    'search': true,
    'url': true,
    'tel': true,
    'email': true,
    'password': true,
    'number': true,
    'date': true,
    'month': true,
    'week': true,
    'time': true,
    'datetime': true,
    'datetime-local': true,
}

/**
 * Computes whether the given element should automatically trigger the
 * `focus-visible` class being added, i.e. whether it should always match
 * `:focus-visible` when focused.
 * @param {Element} node
 * @returns {boolean}
 */
function focusTriggersKeyboardModality(node: Element) {
    const { type, tagName } = node as HTMLInputElement

    if (tagName === 'INPUT' && inputTypesWhitelist[type] && !(node as HTMLInputElement).readOnly) {
        return true
    }

    if (tagName === 'TEXTAREA' && !(node as HTMLInputElement).readOnly) {
        return true
    }

    if ((node as HTMLElement).isContentEditable) {
        return true
    }

    return false
}

/**
 * Keep track of our keyboard modality state with `hadKeyboardEvent`.
 * If the most recent user interaction was via the keyboard;
 * and the key press did not include a meta, alt/option, or control key;
 * then the modality is keyboard. Otherwise, the modality is not keyboard.
 * @param {KeyboardEvent} event
 */
function handleKeyDown(event: KeyboardEvent) {
    if (event.metaKey || event.altKey || event.ctrlKey) {
        return
    }
    hadKeyboardEvent = true
}

/**
 * If at any point a user clicks with a pointing device, ensure that we change
 * the modality away from keyboard.
 * This avoids the situation where a user presses a key on an already focused
 * element, and then clicks on a different element, focusing it with a
 * pointing device, while we still think we're in keyboard modality.
 */
function handlePointerDown() {
    hadKeyboardEvent = false
}

function handleVisibilityChange(this: Document) {
    if (this.visibilityState === 'hidden') {
        // If the tab becomes active again, the browser will handle calling focus
        // on the element (Safari actually calls it twice).
        // If this tab change caused a blur on an element with focus-visible,
        // re-apply the class when the user switches back to the tab.
        if (hadFocusVisibleRecently) {
            hadKeyboardEvent = true
        }
    }
}

function prepare(doc: Document): void {
    doc.addEventListener('keydown', handleKeyDown, true)
    doc.addEventListener('mousedown', handlePointerDown, true)
    doc.addEventListener('pointerdown', handlePointerDown, true)
    doc.addEventListener('touchstart', handlePointerDown, true)
    doc.addEventListener('visibilitychange', handleVisibilityChange, true)
}

export function teardown(doc: Document): void {
    doc.removeEventListener('keydown', handleKeyDown, true)
    doc.removeEventListener('mousedown', handlePointerDown, true)
    doc.removeEventListener('pointerdown', handlePointerDown, true)
    doc.removeEventListener('touchstart', handlePointerDown, true)
    doc.removeEventListener('visibilitychange', handleVisibilityChange, true)
}

export function isFocusVisible(event: React.FocusEvent): boolean {
    const { target } = event
    try {
        return target.matches(':focus-visible')
    } catch (error) {
        // Browsers not implementing :focus-visible will throw a SyntaxError.
        // We use our own heuristic for those browsers.
        // Rethrow might be better if it's not the expected error but do we really
        // want to crash if focus-visible malfunctioned?
    }

    // No need for validFocusTarget check. The user does that by attaching it to
    // focusable events only.
    return hadKeyboardEvent || focusTriggersKeyboardModality(target)
}

export interface IUseIsFocusVisibleResult {
    isFocusVisibleRef: React.MutableRefObject<boolean>
    onBlur: (event: React.FocusEvent<any>) => void
    onFocus: (event: React.FocusEvent<any>) => void
    ref: React.Ref<unknown>
}

export default function useIsFocusVisible(): IUseIsFocusVisibleResult {
    const ref = React.useCallback((node) => {
        if (node !== null) {
            prepare(node.ownerDocument)
        }
    }, [])

    const isFocusVisibleRef = React.useRef(false)

    /**
     * Should be called if a blur event is fired
     */
    function handleBlurVisible() {
        // checking against potential state variable does not suffice if we focus and blur synchronously.
        // React wouldn't have time to trigger a re-render so `focusVisible` would be stale.
        // Ideally we would adjust `isFocusVisible(event)` to look at `relatedTarget` for blur events.
        // This doesn't work in IE11 due to https://github.com/facebook/react/issues/3751
        // TODO: check again if React releases their internal changes to focus event handling (https://github.com/facebook/react/pull/19186).
        if (isFocusVisibleRef.current) {
            // To detect a tab/window switch, we look for a blur event followed
            // rapidly by a visibility change.
            // If we don't see a visibility change within 100ms, it's probably a
            // regular focus change.
            hadFocusVisibleRecently = true
            if (hadFocusVisibleRecentlyTimeout) window.clearTimeout(hadFocusVisibleRecentlyTimeout)
            hadFocusVisibleRecentlyTimeout = window.setTimeout(() => {
                hadFocusVisibleRecently = false
            }, 100)

            isFocusVisibleRef.current = false

            return true
        }

        return false
    }

    /**
     * Should be called if a blur event is fired
     */
    function handleFocusVisible(event: React.FocusEvent) {
        if (isFocusVisible(event)) {
            isFocusVisibleRef.current = true
            return true
        }
        return false
    }

    return { isFocusVisibleRef, onFocus: handleFocusVisible, onBlur: handleBlurVisible, ref }
}

export const forkFocus =
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (rootProps: any, handler: (e: React.FocusEvent) => void) => (e: React.FocusEvent) => {
        if (typeof rootProps.onFocus === 'function') {
            rootProps.onFocus(e)
        }
        handler(e)
    }

export const forkBlur =
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (rootProps: any, handler: (e: React.FocusEvent) => void) => (e: React.FocusEvent) => {
        if (typeof rootProps.onBlur === 'function') {
            rootProps.onBlur(e)
        }
        handler(e)
    }
