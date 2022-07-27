export default function generatePassword(length?: number) {
    const len = length ?? 10
    const charset = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
    let ret = ''
    for (let i = 0, n = charset.length; i < len; ++i) {
        ret += charset.charAt(Math.floor(Math.random() * n))
    }
    return ret
}
