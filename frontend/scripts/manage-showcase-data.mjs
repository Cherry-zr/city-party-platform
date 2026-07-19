import { pbkdf2Sync, randomBytes } from 'node:crypto'
import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { spawn } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const action = process.argv[2]
if (!['seed', 'cleanup'].includes(action)) {
  throw new Error('Usage: node scripts/manage-showcase-data.mjs <seed|cleanup>')
}

const databasePassword = process.env.MYSQL_PASSWORD
if (!databasePassword) throw new Error('Missing MYSQL_PASSWORD for the local showcase database.')

const frontendDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const repositoryDir = path.resolve(frontendDir, '..')
const sqlPath = path.join(repositoryDir, 'database', action === 'seed' ? 'showcase-data.sql' : 'showcase-cleanup.sql')
let sql = await readFile(sqlPath, 'utf8')

if (action === 'seed') {
  const showcasePassword = process.env.SHOWCASE_PASSWORD
  if (!showcasePassword) throw new Error('Missing SHOWCASE_PASSWORD for the local showcase accounts.')
  const salt = randomBytes(16)
  const iterations = 120_000
  const digest = pbkdf2Sync(showcasePassword, salt, iterations, 32, 'sha256')
  const encoded = `pbkdf2$${iterations}$${salt.toString('hex')}$${digest.toString('hex')}`
  sql = `SET @showcase_password_hash = '${encoded}';\n${sql}`
}

const mysql = spawn('mysql', [
  '--default-character-set=utf8mb4',
  `--host=${process.env.MYSQL_HOST || '127.0.0.1'}`,
  `--port=${process.env.MYSQL_PORT || '3306'}`,
  `--user=${process.env.MYSQL_USERNAME || 'city_party'}`,
  `--database=${process.env.MYSQL_DATABASE || 'city_party_platform'}`,
  '--batch'
], {
  env: { ...process.env, MYSQL_PWD: databasePassword },
  stdio: ['pipe', 'inherit', 'inherit'],
  windowsHide: true
})

mysql.stdin.end(sql, 'utf8')

const exitCode = await new Promise((resolve, reject) => {
  mysql.once('error', reject)
  mysql.once('close', resolve)
})

if (exitCode !== 0) throw new Error(`MySQL exited with code ${exitCode}.`)
process.stdout.write(action === 'seed'
  ? 'Local showcase data imported successfully.\n'
  : 'Local showcase data cleaned successfully.\n')
