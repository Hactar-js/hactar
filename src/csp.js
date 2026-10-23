// js-csp compatible shim, re-exported so legacy plugins that do
// `import { put, take, chan, CLOSED } from 'js-csp'` resolve against Hactar's
// in-tree channel. This is a *callback* approximation of js-csp's API, not a
// full reimplementation — it covers the call shapes Hactar plugins use.
//
// Legacy plugins inside sagas typically `yield put(ch, action)`. The core
// auto-iterates generator sagas (see src/plugins.js runSaga) and awaits
// Channel#put for each yielded value, so `yield put(ch, x)` Just Works. This
// module exists for plugins that call js-csp functions directly.

import { Channel, CLOSED } from './channel.js'

export { CLOSED }

export function chan () {
  return new Channel()
}

// `put(ch, value)` in js-csp returns a callback-friendly function; inside a
// generator saga the core sees the yielded value and handles it. To keep the
// legacy call shape we return a descriptor the runner recognises.
export function put (ch, value) {
  return { __hactarPut: true, ch, value }
}