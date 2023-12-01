export enum StoreKey {
    Chat = 'chat-next-web-store',
    Access = 'access-control',
    Config = 'app-config',
    Mask = 'mask-store',
    Prompt = 'prompt-store',
    Update = 'chat-update',
    Sync = 'sync',
}
// eslint-disable-next-line
export const DEFAULT_INPUT_TEMPLATE = `{{input}}` // input / time / model / lang
export const DEFAULT_SYSTEM_TEMPLATE = `
You are ChatGPT, a large language model trained by OpenAI.
Knowledge cutoff: {{cutoff}}
Current model: {{model}}
Current time: {{time}}
Latex inline: $x^2$ 
Latex block: $$e=mc^2$$
`
export const LAST_INPUT_KEY = 'last-input'
