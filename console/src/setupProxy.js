// eslint-disable-next-line @typescript-eslint/no-var-requires
const proxy = require('http-proxy-middleware')

// 'https://virtserver.swaggerhub.com/dreamlandliu/test-mvp/1.0.0/'

// eslint-disable-next-line no-undef
module.exports = (app) => {
    app.use(
        proxy.createProxyMiddleware(['/api'], {
            target: process.env.PROXY || 'http://127.0.0.1:8082',
            // target: process.env.PROXY || 'https://virtserver.swaggerhub.com/dreamlandliu/test-mvp/1.0.0/',
            // pathRewrite: {'/api/v1': '/'},
            changeOrigin: true,
            secure: false,
        })
    )
    // app.use(
    //     proxy.createProxyMiddleware('/ws', {
    //         target: process.env.PROXY.replace('https', 'wss').replace('http', 'ws') || 'ws://127.0.0.1:8082',
    //         ws: true,
    //     })
    // )
}
