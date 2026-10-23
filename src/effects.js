// Declared-effect executor.
//
// Sagas do not perform side effects inline. They yield/return effect
// *descriptors* — plain objects like { type: 'exec', cmd, args } — and the
// core executes them here. Consequences:
//   - plugins are unit-testable without touching the FS or shell
//   - an agent can dry-run a plugin ("plan mode") by collecting effects
//     without executing them
//   - the event log is a faithful replay transcript
//
// Effects supported:
//   { type: 'exec',         cmd, args?, opts? }
//   { type: 'installDeps',  packages: string[], dev?: boolean }
//   { type: 'writeFile',    path, content }
//   { type: 'readFile',     path }
//   { type: 'emitDiagnostic', diag }
//   { type: 'spawn',        fn }            // run a long coroutine (handled by plugins.js)
//   { type: 'log',          message, level? }
//
// Custom effect handlers can be registered via registerHandler.

import { spawn as spawnProc } from 'node:child_process'
import { promises as fs, statSync } from 'node:fs'
import path from 'node:path'

const handlers = new Map()

export function registerHandler (type, fn) {
  handlers.set(type, fn)
}

export function getHandler (type) {
  return handlers.get(type)
}

// Detect the package manager from lockfiles present in cwd.
function detectPackageManager (cwd) {
  if (existsSyncSafe(path.join(cwd, 'pnpm-lock.yaml'))) return 'pnpm'
  if (existsSyncSafe(path.join(cwd, 'yarn.lock'))) return 'yarn'
  return 'npm'
}

function existsSyncSafe (p) {
  try {
    return statSync(p).isFile()
  } catch {
    return false
  }
}

function installCommand (pm, packages, dev, cwd) {
  const devFlag = dev ? (pm === 'yarn' ? '--dev' : '--save-dev') : ''
  const cmd = pm === 'pnpm' ? 'pnpm'
    : pm === 'yarn' ? 'yarn'
    : 'npm'
  const sub = pm === 'pnpm' ? 'add'
    : pm === 'yarn' ? 'add'
    : 'install'
  const args = [sub, ...packages, devFlag].filter(Boolean)
  return { cmd, args, opts: { stdio: 'inherit', cwd } }
}

// Run an effect and return its result. Throws on unknown effect types so
// plugins fail loudly rather than silently no-op.
export async function executeEffect (effect, ctx = {}) {
  if (!effect || typeof effect !== 'object' || !effect.type) {
    throw new Error(`Invalid effect: ${JSON.stringify(effect)}`)
  }
  const handler = handlers.get(effect.type)
  if (!handler) throw new Error(`No handler for effect type "${effect.type}"`)
  return handler(effect, ctx)
}

// Default handlers. Registered at import so callers just use executeEffect.
registerHandler('exec', (eff, ctx) => {
  return new Promise((resolve, reject) => {
    const proc = spawnProc(eff.cmd, eff.args || [], {
      stdio: eff.opts?.stdio ?? 'pipe',
      cwd: eff.opts?.cwd ?? ctx.cwd ?? process.cwd(),
      env: { ...process.env, ...(eff.opts?.env || {}) },
      shell: eff.opts?.shell ?? false,
    })
    let stdout = ''
    let stderr = ''
    if (proc.stdout) proc.stdout.on('data', (d) => { stdout += d })
    if (proc.stderr) proc.stderr.on('data', (d) => { stderr += d })
    proc.on('error', reject)
    proc.on('close', (code) => {
      if (code === 0) resolve({ stdout, stderr, code })
      else {
        const err = new Error(`"${eff.cmd}" exited with code ${code}`)
        err.stdout = stdout
        err.stderr = stderr
        err.code = code
        reject(err)
      }
    })
  })
})

registerHandler('installDeps', async (eff, ctx) => {
  const cwd = ctx.cwd ?? process.cwd()
  const pm = detectPackageManager(cwd)
  const { cmd, args, opts } = installCommand(pm, eff.packages, eff.dev, cwd)
  return executeEffect({ type: 'exec', cmd, args, opts }, ctx)
})

registerHandler('writeFile', async (eff, ctx) => {
  const target = path.resolve(ctx.cwd ?? process.cwd(), eff.path)
  await fs.mkdir(path.dirname(target), { recursive: true })
  await fs.writeFile(target, eff.content, 'utf8')
  return { path: target }
})

registerHandler('readFile', async (eff, ctx) => {
  const target = path.resolve(ctx.cwd ?? process.cwd(), eff.path)
  return fs.readFile(target, 'utf8')
})

registerHandler('emitDiagnostic', async (eff, ctx) => {
  // Diagnostics are published to the channel by the caller (the daemon),
  // which has access to it. The handler just normalizes + returns so tests
  // can assert shape. The daemon wraps this to also put() on the channel.
  return {
    severity: eff.diag.severity || 'error',
    source: eff.diag.source || 'hactar',
    file: eff.diag.file,
    message: eff.diag.message,
    range: eff.diag.range,
  }
})

registerHandler('log', async (eff) => {
  const level = eff.level || 'info'
  // The daemon installs a sink; default goes to stderr so it doesn't break
  // MCP stdio JSON-RPC on stdout.
  const sink = logSink || ((lvl, msg) => process.stderr.write(`[${lvl}] ${msg}\n`))
  sink(level, eff.message)
  return undefined
})

let logSink = null
export function setLogSink (fn) {
  logSink = fn
}

/**
 * Walk an async iterator or generator, executing any yielded effects and
 * feeding their results back as yield values. Used by the saga runner.
 */
export async function runEffectsIterator (iter, ctx) {
  let value = undefined
  let next = iter.next()
  while (!next.done) {
    let yielded = next.value
    // Legacy js-csp `yield put(ch, x)` descriptor → put on the channel.
    if (yielded && yielded.__hactarPut) {
      await yielded.ch.put(yielded.value)
      value = undefined
    } else if (isEffect(yielded)) {
      value = await executeEffect(yielded, ctx)
    } else {
      // Non-effect yield (e.g. a raw promise) — just await it.
      value = await yielded
    }
    next = iter.next(value)
  }
  return next.value
}

function isEffect (v) {
  return v && typeof v === 'object' && typeof v.type === 'string' && handlers.has(v.type)
}