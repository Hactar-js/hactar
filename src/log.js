// Append-only event log. Writes one JSON object per line to
// .hactar/session.jsonl in the project root. Supports replay_session for
// auditable agent sessions.

import { promises as fs, createReadStream } from 'node:fs'
import path from 'node:path'
import { readline } from 'node:readline'

export class EventLog {
  constructor (cwd) {
    this.cwd = cwd ?? process.cwd()
    this.dir = path.join(this.cwd, '.hactar')
    this.file = path.join(this.dir, 'session.jsonl')
    this._ready = null
  }

  async _ensure () {
    if (this._ready) return this._ready
    this._ready = fs.mkdir(this.dir, { recursive: true })
      .then(() => fs.appendFile(this.file, ''))
    return this._ready
  }

  async append (record) {
    await this._ensure()
    const line = JSON.stringify({
      ts: Date.now(),
      ...record,
    })
    await fs.appendFile(this.file, line + '\n', 'utf8')
  }

  /**
   * Async iterate over log entries (oldest first).
   * @returns {AsyncGenerator<object>}
   */
  async * read () {
    let fh
    try {
      fh = await fs.open(this.file, 'r')
    } catch {
      return
    }
    try {
      const rl = readline.createInterface({
        input: fh.createReadStream(),
        crlfDelay: Infinity,
      })
      for await (const line of rl) {
        const trimmed = line.trim()
        if (!trimmed) continue
        try {
          yield JSON.parse(trimmed)
        } catch {
          // skip malformed lines
        }
      }
    } finally {
      await fh.close()
    }
  }
}