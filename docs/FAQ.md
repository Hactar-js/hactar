## Why not use RXJS, redux-saga, or x library?

The API for plugins is intentionally not idiomatic nor elegant. It sacrifices elegance in pursuit of the ultimate pragmatism. The main goal of Hactar is to let you write plugins while you work on projects. While the goals of Hactar could be implemented much cleaner in a library designed for handling asynchronous side-effects it wouldn't be understandable at a glance. I don't want anyone using Hactar to have to read some library's docs to write plugins, however awesome or elegant that library might be.

## How do I create a plugin with multiple reducers or sagas?

You don't. Plugins are designed to be composable; if you want to use multiple sagas or reducers then you simply call them from inside of a higher order function. With sagas it works like this:

```js
function* saga2(action) {
  // do things
}

function* saga1(action) {
  yield saga2(action)
}
```

And with reducers like this:

```js

const subReducer = state, action =>  {
  switch (action.type) {
    case 'DO_THINGS':
      // do things
    default:
      return state
  }
}

const rootReducer = state, action => {
  state = {
    ...state,
    ...subReducer(state, action)
  }
}
```

## I get "maximum call stack size exceeded" or slow installs of Hactar

Hactar unfortunately has some large dependencies (it parses code after all). If it fails due to not getting all its dependencies installed, you can often just run `npm install -g hactar` over and until it works. I'm definitely on this though and will work making the installation work better and learning what the hell I'm doing.
