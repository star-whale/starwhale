/* eslint-disable */
// @ts-nocheck
import React from 'react'
import _ from 'lodash'

type TypeDependency = any[]
type TypeDependencyNames = string

// eslint-disable-next-line
let what_debug_changed = 0
let configuration = { active: true }
function setUseWhatChange({ active = true }: any = {}) {
    configuration = { ...configuration, active }
}

/**
 * Taken random color logic from some stackoverflow answer
 */
function getRandomColor() {
    return '#2B65D9'
    // const letters = '0123456789ABCDEF'
    // let color = '#'
    // for (let i = 0; i < 6; i++) {
    //     color += letters[Math.floor(Math.random() * 16)]
    // }
    // return color
}

/**
 *
 * Check whether the dependency item is an object. then
 */
const isObject = (t: any) => {
    return Object.prototype.toString.call(t) === '[object Object]'
}

function getPrintableInfo(dependencyItem: any) {
    /**
     * Printing the info into viewable format
     */
    if (isObject(dependencyItem) || Array.isArray(dependencyItem)) {
        let ans
        try {
            ans = JSON.stringify(dependencyItem, null, 2)
        } catch (e) {
            ans = 'CIRCULAR JSON'
        }
        return ans
    }

    return dependencyItem
}

// const isDevelopment = process.env['NODE_ENV'] === 'development';

function useHotRefs(value: any) {
    const fnRef = React.useRef(value)
    React.useEffect(() => {
        fnRef.current = value
    })

    return fnRef
}

function useWhatChanged(
    dependency?: TypeDependency,
    dependencyNames?: TypeDependencyNames,
    suffix?: string,
    hookName?: string
) {
    // It's a fair assumption the hooks type will not change for a component during
    // its life time
    const hookNameFinal = React.useMemo(() => {
        if (hookName === 'useLayoutEffect') {
            return 'useLayoutEffect'
        }

        // if(hookName === "useEffect" || !hookName) {
        return 'useEffect'
        // }
    }, [])
    // This ref is responsible for book keeping of the old value
    const dependencyRef = React.useRef(dependency)

    // For count bookkeeping , for easy debugging
    const whatChangedHookCountRef = React.useRef(1)

    // For assigning color for easy debugging
    const backgroundColorRef = React.useRef('')

    const isDependencyArr = Array.isArray(dependencyRef.current)

    React[hookNameFinal](() => {
        if (
            dependencyRef.current &&
            isDependencyArr
            // dependencyRef.current.length > 0
        ) {
            what_debug_changed++

            whatChangedHookCountRef.current = what_debug_changed
            backgroundColorRef.current = getRandomColor()
        }

        // const MyWindow: IWindow = window;
    }, [dependencyRef, isDependencyArr])

    function postConsole() {
        console.log('\n')
        console.log(
            '%c///// END SECTION/////',
            `background: ${backgroundColorRef.current}; color: white; font-size: 10px`,
            '\n'
        )
        console.log('\n')
        console.log('\n')
    }
    function logBanners({
        isFirstMount,
        suffixText,
        isBlankArrayAsDependency,
    }: {
        isFirstMount?: boolean
        suffixText?: string
        isBlankArrayAsDependency?: boolean
    }) {
        if (configuration.active) {
            console.log(
                '%c///// START SECTION /////',
                `background: ${backgroundColorRef.current}; color: white; font-size: 10px`,
                '\n'
            )
            console.log('\n')
            console.log(
                `%c ${whatChangedHookCountRef.current} ${suffix || ''}`,
                `background: ${backgroundColorRef.current}; color: white; font-size: 10px`,
                '👇🏾',
                `${isFirstMount ? 'FIRST RUN' : 'UPDATES'}`,
                `${suffixText}`
            )

            if (isBlankArrayAsDependency) {
                postConsole()
            }
        }
    }

    const longBannersRef = useHotRefs(logBanners)

    React[hookNameFinal](() => {
        if (!(dependencyRef.current && isDependencyArr)) {
            return
        }

        // if (dependencyRef.current.length === 0) {
        //   return;
        // }

        // More info, if needed by user
        const stringSplitted = dependencyNames ? dependencyNames.split(',') : null
        let changed = false
        const whatChanged = dependency
            ? dependency.reduce((acc, dep, index) => {
                  if (dependencyRef.current && dep !== dependencyRef.current[index]) {
                      const oldValue = dependencyRef.current[index]
                      dependencyRef.current[index] = dep
                      if (dependencyNames && stringSplitted) {
                          changed = true
                          acc[`"✅" ${stringSplitted[index]}`] = {
                              'Old Value': getPrintableInfo(oldValue),
                              'New Value': getPrintableInfo(dep),
                          }
                      } else {
                          acc[`"✅" ${index}`] = {
                              'Old Value': getPrintableInfo(oldValue),
                              'New Value': getPrintableInfo(dep),
                          }
                      }

                      return acc
                  }
                  if (dependencyNames && stringSplitted) {
                      acc[`"⏺" ${stringSplitted[index]}`] = {
                          'Old Value': getPrintableInfo(dep),
                          'New Value': getPrintableInfo(dep),
                      }
                  } else {
                      acc[`"⏺" ${index}`] = {
                          'Old Value': getPrintableInfo(dep),
                          'New Value': getPrintableInfo(dep),
                      }
                  }

                  return acc
              }, {})
            : {}
        if (configuration.active) {
            const isBlankArrayAsDependency = whatChanged && Object.keys(whatChanged).length === 0 && isDependencyArr
            longBannersRef.current({
                isFirstMount: !changed,
                suffixText: isBlankArrayAsDependency ? ' 👉🏽 This will run only once on mount.' : '',
                isBlankArrayAsDependency,
            })

            if (!isBlankArrayAsDependency) {
                console.table(whatChanged)
                postConsole()
            }
        }
    }, [
        ...(() => {
            if (dependency && isDependencyArr) {
                return dependency
            }
            return []
        })(),
        dependencyRef,
        longBannersRef,
        hookName,
    ])
}

export function useIfChanged(data: any) {
    let dependency = null
    let dependencyNames

    if (_.isArray(data)) {
        dependency = data
    } else if (_.isObject(data)) {
        dependency = _.values(data)
        dependencyNames = _.keys(data).join(',')
    } else {
        dependency = [data]
    }

    return useWhatChanged(dependency, dependencyNames)
}

export { useWhatChanged, setUseWhatChange }
