import { readdir, readFile } from 'node:fs/promises';
import { extname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const SOURCE_ROOT = fileURLToPath(new URL('../src/', import.meta.url));
const TEXT_EXTENSIONS = new Set(['.css', '.html', '.js', '.json', '.jsx', '.ts', '.tsx']);
const MOJIBAKE_MARKERS = [
  'Ã', 'Ä‘', 'Ä', 'Ă´', 'Ă£', 'Ă¡', 'Ă¢', 'Ă ', 'Ă¹', 'Ăº', 'Ăª', 'Ă©', 'Ă¬', 'Ă­',
  'Æ°', 'Æ¡', 'áº', 'á»', 'â€¢', 'Â·', '�',
];

async function collectFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const nested = await Promise.all(entries.map(async (entry) => {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) return collectFiles(path);
    return TEXT_EXTENSIONS.has(extname(entry.name)) ? [path] : [];
  }));
  return nested.flat();
}

const files = await collectFiles(SOURCE_ROOT);
const failures = [];

for (const file of files) {
  const content = await readFile(file, 'utf8');
  content.split(/\r?\n/).forEach((line, index) => {
    const marker = MOJIBAKE_MARKERS.find((candidate) => line.includes(candidate));
    if (marker) {
      failures.push(`${relative(SOURCE_ROOT, file)}:${index + 1} chứa dấu hiệu mojibake “${marker}”`);
    }
  });
}

if (failures.length) {
  console.error('Phát hiện chuỗi có dấu hiệu sai encoding:\n');
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exitCode = 1;
} else {
  console.log('Encoding check passed.');
}
