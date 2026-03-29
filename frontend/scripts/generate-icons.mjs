import sharp from 'sharp'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const publicDir = path.resolve(__dirname, '..', 'public')
const src = path.join(publicDir, 'favicon.png')

async function generatePng(size, outName) {
  // Trim transparent padding, then cover-resize into a square.
  // This yields a tighter, more legible favicon in browser tabs.
  await sharp(src)
    .trim()
    .resize(size, size, { fit: 'cover', position: 'centre' })
    .png({ compressionLevel: 9 })
    .toFile(path.join(publicDir, outName))
}

async function main() {
  await generatePng(16, 'favicon-16.png')
  await generatePng(32, 'favicon-32.png')
  await generatePng(48, 'favicon-48.png')

  // Also generate a crisp navbar logo (32px height baseline).
  await sharp(src)
    .trim()
    .resize(64, 64, { fit: 'cover', position: 'centre' })
    .png({ compressionLevel: 9 })
    .toFile(path.join(publicDir, 'logo.png'))
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})

