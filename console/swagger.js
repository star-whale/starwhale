const { generateApi } = require('swagger-typescript-api')
const path = require('path')
const fs = require('fs')

/* NOTE: all fields are optional expect one of `output`, `url`, `spec` */
generateApi({
    name: 'MySuperbApi.ts',
    output: path.resolve(process.cwd(), './src/__generated__'),
    // url: 'http://api.com/swagger.json',
    input: path.resolve(process.cwd(), './swagger.json'),
    // spec: {
    //     swagger: '3.0',
    //     info: {
    //         version: '1.0.0',
    //         title: 'Swagger Petstore',
    //     },
    //     // ...
    // },
    // templates: path.resolve(process.cwd(), './api-templates'),
    httpClientType: 'axios', // or "fetch"
    defaultResponseAsSuccess: false,
    generateRouteTypes: false,
    generateResponses: true,
    toJS: false,
    extractRequestParams: false,
    extractRequestBody: false,
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
    // .then(({ files, configuration }) => {
    //     files.forEach(({ content, name }) => {
    //         fs.writeFile(path, content, (err) => {
    //             if (err) console.log(err)
    //         })
    //     })
    // })
    .catch((e) => console.error(e))
