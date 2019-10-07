 # Hactar

*warning*: Hactar is in the very early alpha stages and since it is a tool that modifies your code it is very important you run it against stuff that has backups. It likely wont make anything explode but you could lose work.

*Note*: This branch contains a complete rewrite and rethinking of Hactar.

Hactar is the solution to [JavaScript Fatigue](https://medium.com/@ericclemmons/javascript-fatigue-48d4011b6fc4). Hactar configures build tools, installs dependencies, adds imports, creates tests etc, all automatically. There are no boilerplates to clone, no generators to run, and no build tools to configure. To use Hactar you simply start writing code and Hactar figures out what you want to do and the best practices to make it happen. Start writing ES6 and it will add Babel, start writing sass and it will add node-sass, import an image and it will add Webpack etc

No more starting projects with configuration and boilerplate, Hactar let's you start by writing code.

Hactar can currently;

- Automatically install dependencies
- Automatically configure next plugins
  - next-css 
  - next-mdx
  - next-org

Hactar does this all without any interaction from you. Hactar parses your code, figures out what you are coding, then installs, configures, and writes code to make it work. You start writing code and Hactar does the rest.

A typical Hactar workflow looks like this;

Run Hactar:

```sh
$ hactar
```

Now start coding:

```sh
$ touch pages/index.js
```

```js
import React from 'react';

const Index = () => (
  <div>
    <p>Hello Next.js</p>
  </div>
);

export default Index;
```

Hactar will parse the code and detect a React component in /pages and assume you are using next:

```sh
$ installing and configuring next
```

Now say you add a logo to your page like this:

```js
import Logo from "../assets/logo.svg"
const Index = () => (
  <div>
    <Logo />
    <p>Hello Next.js</p>
  </div>
);

export default Index;
```

Hactar will detect this and add _next-svg_:

```sh
$ installing and configuring next-svg
```

## Table of Contents

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Installation](#installation)
- [Usage](#usage)
- [Configuring](#configuring)
- [The Principles of Hactar](#the-principles-of-hactar)
	- [1. Stay out of your way](#1-stay-out-of-your-way)
	- [2. Be massively hackable](#2-be-massively-hackable)
	- [3. Do not be a dependency](#3-do-not-be-a-dependency)
	- [4. Lots of small wins](#4-lots-of-small-wins)
- [How Hactar Works](#how-hactar-works)
- [Extending Hactar](#extending-hactar)
- [License](#license)

<!-- /TOC -->

## Installation

Because Hactar is designed to be extended by a user it is installed by cloning.

```sh
git clone https://github.com/Hactar-js/hactar ~/.hactar
```

Then add ~/.hactar/bin/hactar to your path:

With bash or zsh: 

```sh
$ export PATH=$PATH:~/.hactar/bin
```

With fish:

```sh
$ set -gx PATH ~/.hactar $PATH
```

You might need to chmod the bin:

```sh
$ chmod +x ~/.hactar/bin/hactar
```

## Usage

There are three ways to interact with Hactar;

1. Just run the hactar command, hactar will then, through the magic of parsing, make intelligent guesses about what you are trying.
2. Manually running commands like add, list, remove etc.
3. Configuring 

Currently there are two commands you can use:

1. `$ hactar list` to list all the behaviors Hactar provides
2. `$ hactar add` to add a behavior to a project

### Listing Behaviors

You can list all behaviors with:

```sh
$ hactar list
```

If you want to select one of these and quickly install I highly recommend using with fzf. Here is a function in fish that wraps them:

```fish
function h -d "find and add hactar dependency"
    hactar list | fzf | string match -r -a '^[:](\w+(?:-\w+)*|\$[\d.]+|\S+)' $1 | awk 'NR == 2' | string trim | read -l result; and hactar add "$result"
end
```

### Adding Behaviors

You can add a behavior to a project using `hactar add` e.g:

```sh
$ hactar add next-svg # adds next-svg support
```

## Configuring

Hactar is configured by placing a `hactar.edn` file in the project's root. All keys in the edn file should correspond to behavior names, you can see the documentation for a behavior to discover it's configuration options. Here is an example next-css configured to use css-modules:

```edn
{:next-css {:css-modules true
            :nesting true}}
```

## The Principles of Hactar

### 1. Stay out of your way

Hactar is not a boilerplate and it is not a scaffolder. You don't have to run Hactar every time you need to create a new _thing_ with a new _thing_. If something is gonna need tests, Hactar will figure it out through parsing, no need for you to tell it. And when conventions change, Hactar will automatically refactor your code using codemods; no interaction from you.

### 2. Be massively hackable

Hactar is written in ClojureScript and cloned to your system, it is design to be coded on while running and continually extended by a user. The goal is that you encode conventions for you how configure your projects once and then re-use them via Hactar.

### 3. Do not be a dependency

There are many solutions to JavaScript Fatigue but most require you to _adopt_ them and without them your code becomes useless, unable to be used without the solution. And if you want someone else to contribute to the code, they now need to learn the tool and its ecosystem.

> When your solution to fatigue is a dependency the solution can become the fatigue.

Because Hactar simply writes code, your code is not dependent on it. Nothing Hactar does is dependent on Hactar to work. No one contributing to your code even need know Hactar exists. Hactar is transparent and designed to fade into the background. It is just another coder on your team -- one you pay with CPU. If Hactar stops being useful you can simply fire it.

### 4. Lots of small wins

Hactar is immediately beneficial today. Hactar is oriented towards tiny things that do one thing well (like for example, adding babel support). You don't need a ton of plugins for it to come together and work for you. It has a ton of little things that make your life better now. 

## Behaviors 

Hactar currently supports the following:

- next. Hactar will automatically detect next projects and usage of the following:
  - mdx. Mdx is automatically detected and support for it added.
  - css. CSS usage is detected and next-css is added with default config.
  - svg. SVG usage is detected and next-svg is added with default config. 
    
## How Hactar Works

## Extending Hactar

## License

ISC
