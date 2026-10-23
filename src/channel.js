// In-tree unbounded async FIFO channel.
//
// Replaces the unmaintained js-csp dependency with ~40 LOC that cover the
// shape Hactar actually uses: put / take / close / CLOSED. A tiny js-csp
// compatible shim (put/take/chan/CLOSE) is re-exported from src/csp.js so
// legacy plugins that `import { put } from 'js-csp'` keep working.

export const CLOSED = Symbol('hactar:CLOSED')

export class Channel {
  constructor () {
    this._closed = false
    // Unbuffered-but-promise-aware: queued puts resolve once a taker arrives.
    this._takes = [] // pending take() resolvers
    this._puts = [] // pending put() values awaiting a taker
  }

  /**
   * Place a value on the channel. Resolves when the value has been taken
   * (or immediately, if a taker is already waiting). Resolves immediately
   * with undefined if the channel is closed.
   * @param {*} value
   * @returns {Promise<void>}
   */
  put (value) {
    if (this._closed) return Promise.resolve()
    const taker = this._takes.shift()
    if (taker) {
      taker.resolve(value)
      return Promise.resolve()
    }
    return new Promise((resolve) => {
      this._puts.push({ value, resolve })
    })
  }

  /**
   * Take a value from the channel. Resolves with the value, or CLOSED once
   * the channel is closed and drained.
   * @returns {Promise<*>}
   */
  take () {
    if (this._puts.length > 0) {
      const { value, resolve } = this._puts.shift()
      resolve()
      return Promise.resolve(value)
    }
    if (this._closed) return Promise.resolve(CLOSED)
    return new Promise((resolve) => {
      this._takes.push({ resolve })
    })
  }

  close () {
    if (this._closed) return
    this._closed = true
    while (this._takes.length) this._takes.shift().resolve(CLOSED)
  }

  get closed () {
    return this._closed
  }
}

export function chan () {
  return new Channel()
}