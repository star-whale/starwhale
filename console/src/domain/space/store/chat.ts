import { createPersistStore } from '@starwhale/ui/Serving/utils/store'

const DEFAULT_STATE = {}

export const useChatStore = createPersistStore(
    DEFAULT_STATE,
    (set, _get) => {
        function get() {
            return {
                ..._get(),
                // eslint-disable-next-line
                ...methods,
            }
        }

        const methods = {}

        return methods
    },
    {
        name: 'chat',
        version: 1,
    }
)
