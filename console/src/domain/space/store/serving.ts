import { createPersistStore } from '@starwhale/ui/Serving/utils/store'

const DEFAULT_STATE = {}

export const useOnlneEvalStore = createPersistStore(
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
        name: 'fine-tune-seving',
        version: 1,
    }
)