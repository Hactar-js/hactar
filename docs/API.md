## API Reference

Hactar has a very minimal API by design

## CLI Options

### plugins, -p

Plugins to install using npm. Any plugins passed to it are added to devDependencies in package.json. You can install plugins the exact same way using npm:

```sh
$ npm install --save-dev hactar-plugin-name
```

## Plugins

### function* saga(action, channel, getState)

A saga is a generator function that receives Flux actions, a channel, and state (set by other plugins)

#### Arguments

1. action: A Flux standard action with a `type` and `payload` keys. For example:

```js
{type: 'ADD_FILE', payload; { filepath: '~/project/src/index.js'}}
```

2. channel: A [js-csp](https://github.com/ubolonton/js-csp) channel. See the js-csp docs for the shape of its [API](https://github.com/ubolonton/js-csp/blob/master/doc/basic.md#channels)

3. getState: A redux getState function. Returns the current state of Hactar that plugins have added to. See the Redux docs for how [its API](https://github.com/reactjs/redux/blob/master/docs/api/Store.md#getState)

#### Returns

(Nothing): Sagas should only dispatch actions.

### function reducer(state, action)

A reducer function. They are the same as Redux reducers, so read its [docs](https://github.com/reactjs/redux/blob/master/docs/Glossary.md#reducer)
