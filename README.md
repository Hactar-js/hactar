# Hactar

*warning*: Hactar is in the very early alpha stages and since it is a tool that modifies your code it is very important you run it against stuff that has backups. It likely wont make anything explode but you could lose work.

Hactar is the solution to [JavaScript Fatigue](https://medium.com/@ericclemmons/javascript-fatigue-48d4011b6fc4). Hactar configures build tools, installs dependencies, adds imports, creates tests etc, all automatically. There are no boilerplates to clone, no generators to run, and no build tools to configure. To use Hactar you simply start writing code and Hactar figures out what you want to do and the best practices to make it happen. Start writing ES6 and it will add Babel, start writing sass and it will add node-sass, import an image and it will add Webpack etc

No more starting projects with configuration and boilerplate, Hactar let's you start writing code immediately.

Hactar can currently;

- Automatically install dependencies
- Detect ES6 and add Babel transpilation
- Detect experimental ES6 features and configure Babel presets like stage-0
- Automatically detect React and add babel-react plugins

Hactar does this all without any interaction from you. Hactar parses your code, figures out what you are coding, then installs, configures, and writes code to make it work. You start writing code and Hactar does the rest.

Here is a [screencast](https://www.dropbox.com/s/7lwd3efcqh4scl4/Hactar-Screencast.mp4?dl=0) for the visual learners among us

A typical Hactar workflow looks like this;

Run Hactar:

```sh
$ hactar -p hactar-babel
initiating npm
name: (testcats)
...
hactar is running
```

Now start coding:

```sh
$ touch src/index.js
$ atom .
```

```js
import React from 'react';
import Button from 'react-toolbox/lib/button';

const CustomButton = () => (
  <Button label="Hello world" raised accent />
);

export default CustomButton;
```

Hactar will parse the code and detect the usage of ES6, React, and react-toolbox:

```sh
installing babel
configuring babel with es2015
installing react
installing react-toolbox
```

## Table of Contents

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Installation](#installation)
- [Usage](#usage)
- [The Principles of Hactar](#the-principles-of-hactar)
	- [1. Stay out of your way](#1-stay-out-of-your-way)
	- [2. Be massively hackable](#2-be-massively-hackable)
	- [3. Do not be a dependency](#3-do-not-be-a-dependency)
	- [4. Lots of small wins](#4-lots-of-small-wins)
- [Presets and Plugins](#presets-and-plugins)
- [How Hactar Works](#how-hactar-works)
- [Documentation](#documentation)
- [License](#license)
- [Support](#support)

<!-- /TOC -->

## Installation

Install globally using:

```sh
$ npm install -g hactar
```

## Usage

Hactar is designed to have almost no interaction. There are no generators you can execute nor things to configure. To use Hactar, you simply run the `hactar` command. The only option available to you is `--plugins`, which you can use to install various Hactar plugins:

```sh
$ hactar --plugins plugin-name,plugin-name
```

You can also install a plugin simply by adding it to your dependencies (which is what the --plugins argument does)

```sh
$ npm install --save-dev hactar-babel
```

## The Principles of Hactar

### 1. Stay out of your way

Hactar is not a boilerplate and it is not a scaffolder. You don't have to run Hactar every time you need to create a new _thing_ with a new _thing_. If something is gonna need tests, Hactar will figure it out through parsing, no need for you to tell it. And when conventions change, Hactar will automatically refactor your code using codemods; no interaction from you.

### 2. Be massively hackable

Hactar plugins are simple ES6 generator functions so you already know how to write them. There are no unfamiliar models like streams, transforms, pipes etc to learn. Writing Hactar plugins feels as productive as writing shell scripts but better. You can code plugins for Hactar while you work on your projects -- building solutions to fatigues as they occur.

### 3. Do not be a dependency

There are many solutions to JavaScript Fatigue but most require you to _adopt_ them and without them your code becomes useless, unable to be used without the solution. And if you want someone else to contribute to the code, they now need to learn the tool and its ecosystem.

When your solution to fatigue is a dependency the solution can become the fatigue.

Because Hactar simply writes code, your code is not dependent on it. Nothing Hactar does is dependent on Hactar to work. No one contributing to your code even need know Hactar exists. Hactar is transparent and designed to fade into the background. It is just another coder on your team -- one you pay with CPU. If Hactar stops being useful you can simply fire it.

### 4. Lots of small wins

Hactar is immediately beneficial today. Hactar is oriented towards tiny plugins that do one thing well (like for example, adding babel support). You don't need a ton of plugins for it to come together and work for you. It has a ton of little things that make your life better now. Too many solutions to fatigue are "all or nothing" propositions that require huge wins before the little wins. How many have set out to solve their fatigue only to realize 6 months later things that the ecosystem has changed too much making it useless, or that it was too ambitious, so they give up and return to what works good enough. Hactar is not like that, it comes with little wins today and can be grown to be so much more. Hactar evolves fast and is designed to be changeable and hackable, even while you work on your projects. Every plugin is designed to improve your coding experience in some tiny way; whether it is extracting tests from comments or automatically adding a preset to babel. It is always useful now not later.

## Plugins

Hactar currently has the following plugins;

- [hactar-auto-install](https://github.com/Hactar-js/hactar-auto-install) A plugin that parses your imports and automatically installs missing dependencies.
- [hactar-babel](https://github.com/Hactar-js/hactar-babel) Provides all the babel plugins that do things like configure ES62105 preset, detect stage-0 features, react etc.

You can find all the existing Hactar plugins by searching for _hactar_ on [npm](https://www.npmjs.com/search?q=hactar)

## How Hactar Works

There are four parts to Hactar;

1. A filesystem watcher (uses [chokidar](https://github.com/paulmillr/chokidar))
2. A CSP like Flux dispatcher + Redux store
3. Generator functions and reducers, which make up the plugins.
4. Parsers and codemods. Most plugins in Hactar make use of a JS parser such as Espree and codemod tools like jscodeshift

Every plugin in Hactar receives all the actions and can dispatch actions to all other plugins via a channel. Plugins are split into two parts;

1. Reducers which can be used to store state
2. Sagas that can be used to dispatch actions and make asynchronous modifications to the codebase

Sagas are generator functions that run on a loop for as long as Hactar is running.

A plugin that adds a index.js file when Hactar is loaded would look like this:

```js
import { put } from 'js-csp'

function* saga(action, ch) {
  if(action.type == 'INITIALIZE') {
    // Dispatch an ADD_FILE action for an addFile plugin to pick up
    yield put(ch, {type: 'ADD_FILE', name: 'index.js', contents: `console.log('Hello World!')`})
  }
}

export { saga }
```

And we could handle storing state and getting state by doing the following

```js
import { put } from 'js-csp'

const reducer = (state, action) => {
  switch (action.type) {
    case 'DOGS_R_AWESOME':
      return {
        ...state,
        dogs: 'Are Awesome'
      }
    default:
      return state
  }
}

function* saga(action, ch, getState) {
  if(getState().hasDogs) {
    yield put(ch, {type: 'DOGS_R_AWESOME'})
  }
}

export { reducer, saga }
```

Hactar is designed to be insanely easy to make plugins for. The hope is that this will encourage you to solve fatigues when you experience them and not later _when you can get around to it_. If something annoys you and you feel it could be automated away it shouldn't take learning a new ecosystem to write a solution, it should just be a matter of coding a solution. If you can write ES6 code you can write a Hactar plugin. I feel very strongly that the process for automating something should be _write code_ and not install x, configure y, read the docs on z, and cuss at...

See the [documentation](https://hactar-js.github.io/hactar) for more examples

## Documentation

- [Building a Plugin](https://hactar-js.github.io/hactar/#building-a-plugin)
- [API Reference](https://hactar-js.github.io/hactar/API)
- [FAQ](https://hactar-js.github.io/hactar/FAQ)

More documentation coming soon!

## License

ISC

## Support

If you found this repo useful please consider supporting me on [Gratipay](https://gratipay.com/~k2052/), sending me some bitcoin `1csGsaDCFLRPPqugYjX93PEzaStuqXVMu`, or giving me lunch money via [Cash.me/$k2052](https://cash.me/$k2052) or [paypal.me/k2052](http://paypal.me/k2052)
