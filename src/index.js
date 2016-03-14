import 'babel-polyfill'
import fs from 'fs'
import path from 'path'
import { execSync } from 'child_process'
import program from 'commander'
import chokidar from 'chokidar'
import chalk from 'chalk'
import parse from 'gitignore-globs'
import { createStore } from 'redux'
import { go, spawn, putAsync, CLOSED, chan } from 'js-csp'

const list = val => val.split(',')

program
  .version('0.0.5')
  .option('-p, --plugins <plugins>', 'Set plugins', list)
  .parse(process.argv)

// does a packagejson exist? if not we create one
const packagejsonPath = path.join(process.cwd(), '/package.json')
let packagejson
try {
  fs.accessSync(packagejsonPath, fs.F_OK)
  packagejson = require(packagejsonPath)
} catch (e) {
  console.log(chalk.yellow('initiating npm'))
  execSync('npm init', { stdio: [0, 1, 2] })
  packagejson = require(packagejsonPath)
}

// Handle ignoring stuff like node_modules
const gitIgnorePath = path.join(process.cwd(), '.gitignore')
let ignored
try {
  fs.accessSync(gitIgnorePath, fs.F_OK)
  ignored = parse(gitIgnorePath)
} catch (e) {
  ignored = [/node_modules/]
}

// install plugins and wait for them to finish installing
if (program.plugins) {
  console.log(`installing plugins; ${program.plugins.join(' ')}`)
  execSync(`npm install --save-dev ${program.plugins.join(' ')}`, { stdio: [0, 1, 2] })
}

const flatten = a =>
  a.reduce(
    (flat, next) => flat.concat(Array.isArray(next) ? flatten(next) : next), []
  )

const requirePlugin = plugin => require(path.join(process.cwd(), 'node_modules', plugin))

const requirePlugins = (pkg, blacklist = []) =>
  !pkg ? []
    : flatten(['dependencies', 'devDependencies', 'peerDependencies']
      .filter((key) => key in pkg)
      .map((dep) => Object.keys(pkg[dep])))
      .filter((dep) => /^hactar-[-\w]+/g.test(dep))
      .filter((dep) => !~blacklist.indexOf(dep))
      .reduce((prev, next) => prev.concat(requirePlugin(next)), [])

let plugins = []

// Push reducers and sagas from the plugins
const loadPluginsFromPackageJSON = () => {
  let _plugins = requirePlugins(packagejson)
  if (_plugins.length === 0) {
    execSync('npm install --save-dev hactar-auto-install@latest', { stdio: [0, 1, 2] })
    _plugins = requirePlugins(packagejson)
  }

  return _plugins
}

plugins = loadPluginsFromPackageJSON()

// load a local file with custom plugins
const hactarCustomizationPath = path.join(process.cwd(), '/hactar.config-plugin.js')

let sagas = []
let reducers = []
const addReducersAndSagasFromPlugins = (requiredPlugins) => {
  const _sagas = []
  const _reducers = []
  requiredPlugins.forEach((plugin) => {
    if (plugin.reducer) {
      _reducers.push(plugin.reducer)
    }

    if (plugin.saga) {
      _sagas.push(plugin.saga)
    }
  })

  return [_sagas, _reducers]
}

[sagas, reducers] = addReducersAndSagasFromPlugins(plugins)

const tryLoadHactarConifg = () => {
  try {
    fs.accessSync(hactarCustomizationPath, fs.F_OK)
    plugins = [...loadPluginsFromPackageJSON(), ...require(hactarCustomizationPath)]
    console.log(chalk.green('loaded Hactar config'))
    const sagasAndReducers = addReducersAndSagasFromPlugins(plugins)
    sagas = sagasAndReducers[0]
    reducers = sagasAndReducers[1]
  } catch (e) {
    // TODO: Better Error message
    console.log(chalk.red(`failed to load Hactar config. we got the following error ${e.message}`))
  }
}

// Core reducer simply calls all the reducers in plugins
const rootReducer = (state, action) => {
  let newState = state

  reducers.forEach((reducer) => {
    newState = {
      ...state,
      ...reducer(state, action),
    }
  })

  return newState
}

const store = createStore(rootReducer)
const ch = chan()

const dispatch = (event) => {
  sagas.forEach((saga) => {
    spawn(saga(event, ch, store.getState))
  })

  store.dispatch(event)
}

function* pluginRunner() {
  let value = yield ch

  while (value !== CLOSED) {
    dispatch(value)
    value = yield ch
  }
}

go(pluginRunner)

chokidar.watch(process.cwd(), { ignored }).on('all', (event, filePath) => {
  // Reload hactar
  // TODO: make this work with plugins running on a loop
  if (filePath.indexOf('hactar.config-plugin.js') > -1) {
    console.log('loading Hactar config')
    delete require.cache[hactarCustomizationPath]
    tryLoadHactarConifg()
  }

  switch (event.toUpperCase()) {
    case 'ADD':
      putAsync(ch, { type: 'ADD_FILE', path: filePath })
      break
    case 'CHANGE':
      putAsync(ch, { type: 'CHANGED_FILE', path: filePath })
      break
    default:
      putAsync(ch, { type: event.toUpperCase(), path: filePath })
  }
})

console.log(chalk.green('Hactar is Running'))
