import { create } from 'zustand'
import { combine, persist } from 'zustand/middleware'
import { Updater } from './typing'
import { deepClone } from './clone'

// type SecondParam<T> = T extends (_f: infer _F, _s: infer S, ...args: infer _U) => any ? S : never

type MakeUpdater<T> = {
    lastUpdateTime: number

    markUpdate: () => void
    update: Updater<T>
}

type SetStoreState<T> = (
    partial: T | Partial<T> | ((state: T) => T | Partial<T>),
    replace?: boolean | undefined
) => void

export function createPersistStore<T extends object, M>(
    state: T,
    methods: (set: SetStoreState<T & MakeUpdater<T>>, get: () => T & MakeUpdater<T>) => M,
    // persistOptions: SecondParam<typeof persist<T & M & MakeUpdater<T>>>
    persistOptions: any
) {
    return create(
        persist(
            combine(
                {
                    ...state,
                    lastUpdateTime: 0,
                },
                (set, get) => {
                    return {
                        ...methods(set, get as any),

                        markUpdate() {
                            set({ lastUpdateTime: Date.now() } as Partial<T & M & MakeUpdater<T>>)
                        },
                        update(updater) {
                            const _state = deepClone(get())
                            updater(_state)
                            set({
                                ..._state,
                                lastUpdateTime: Date.now(),
                            })
                        },
                    } as M & MakeUpdater<T>
                }
            ),
            persistOptions as any
        )
    )
}
