const { generateApi, generateTemplates } = require('swagger-typescript-api')
const path = require('path')
const fs = require('fs')

const PATH_TO_OUTPUT_DIR = path.resolve(process.cwd(), './src/api/server')
const PATH_TO_OUTPUT_DIR_TEMPLATE = path.resolve(process.cwd(), './src/api/template')

/* NOTE: all fields are optional expect one of `output`, `url`, `spec` */
generateApi({
    name: 'MySuperbApi.ts',
    output: PATH_TO_OUTPUT_DIR,
    url: 'http://10.0.128.11:8082/v3/api-docs/Api',
    // input: path.resolve(process.cwd(), './swagger.json'),
    // spec: {
    //     swagger: '3.0',
    //     info: {
    //         version: '1.0.0',
    //         title: 'Swagger Petstore',
    //     },
    //     // ...
    // },
    templates: PATH_TO_OUTPUT_DIR_TEMPLATE,
    httpClientType: 'axios', // or "fetch"
    defaultResponseAsSuccess: false,
    generateRouteTypes: false,
    generateResponses: true,
    toJS: false,
    extractRequestParams: false,
    extractRequestBody: false,
    unwrapResponseData: true,
    prettier: {
        // By default prettier config is load from your project
        printWidth: 120,
        tabWidth: 2,
        trailingComma: 'all',
        parser: 'typescript',
    },
    defaultResponseType: 'void',
    singleHttpClient: true,
    cleanOutput: false,
    enumNamesAsValues: false,
    moduleNameFirstTag: false,
    generateUnionEnums: false,
    extraTemplates: [],
    modular: true,
    extractResponseBody: true,
    hooks: {
        onCreateComponent: (component) => {},
        onCreateRequestParams: (rawType) => {},
        onCreateRoute: (routeData) => {},
        onCreateRouteName: (routeNameInfo, rawRouteInfo) => {},
        onFormatRouteName: (routeInfo, templateRouteName) => {},
        onFormatTypeName: (typeName, rawTypeName) => {},
        onInit: (configuration) => {},
        onParseSchema: (originalSchema, parsedSchema) => {},
        onPrepareConfig: (currentConfiguration) => {},
    },
})
    .then(({ files, configuration }) => {
        // console.log(files)
        // files.forEach(({ content, name }) => {
        //     fs.writeFile(path, content, (err) => {
        //         if (err) console.log(err)
        //     })
        // })
    })
    .catch((e) => console.error(e))

// generateTemplates({
//     cleanOutput: false,
//     output: PATH_TO_OUTPUT_DIR_TEMPLATE,
//     httpClientType: 'axios',
//     modular: true,
//     silent: false,
//     rewrite: false,
// })
