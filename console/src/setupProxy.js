// eslint-disable-next-line @typescript-eslint/no-var-requires
const proxy = require('http-proxy-middleware')

// eslint-disable-next-line no-undef
module.exports = (app) => {
    app.use(
        proxy.createProxyMiddleware('/api/v1/log/online', {
            changeOrigin: true,
            target: process.env.PROXY.replace('https', 'wss').replace('http', 'ws') || 'ws://127.0.0.1:8082',
            ws: true,
        })
    )
    app.use(
        proxy.createProxyMiddleware(['/api'], {
            target: process.env.PROXY || 'http://127.0.0.1:8082',
            // pathRewrite: {'/api/v1': '/'},
            changeOrigin: true,
            secure: false,
        })
    )
}
