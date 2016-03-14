## Building a Plugin

Developing a plugin for Hactar is easy. The recommended way to start a plugin is by creating a Hactar plugin file `hactar.config-plugin.js` on a project. This lets you write solutions to fatigues while you code. If you run into something that could be automated you simply add it to the config file and rip it out into a plugin later.

Plugins consist of two parts;

1. Sagas that handle asynchronous actions
2. Redux reducers which modify state

Both of these are optional. A valid plugin exports these two functions like this:

```js
export { saga, reducer }
```

Let's start our first plugin. Create a file called `hactar.config-plugin.js` in the root of your project with the following:

```js
// Prints hello world to the console
function* helloWorld() {
  console.log('hello world')
}

export { saga: helloWorld }
```

A Hactar config has the same shape as a plugin. This makes it easy to develop a plugin (or plugins) that are specific to a project or to develop a plugin without hassling with npm links.

When you are ready to you can simply rip out the plugin, give it a package.json and you are done.

## Communicating With Other Plugins

There are two ways to communicate between plugins:

1. Add state via reducer
2. Dispatch an action to a channel

Reducers work exactly like Redux reducers. If we wanted to for example, add a key with details about cats we could do the following:

```js
const reducer = state, action => {
  switch (action.type) {
    case 'INITIALIZE':
      return {
        ...state,
        cats: 'r awesome'
      }
    default:
      return state
  }
}
```

Every saga receives a channel as its second argument, you can put any action onto this channel and another plugin's saga or reducer can then pick it up:

```js
function* saga(action, ch, getState) {
  if(getState().hasDogs) {
    yield put(ch, {type: 'DOGS_R_AWESOME'})
  }
}
```
