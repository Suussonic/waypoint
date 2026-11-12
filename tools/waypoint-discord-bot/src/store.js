import { mkdir, readFile, rename, writeFile } from 'node:fs/promises';
import { join } from 'node:path';

const directory = process.env.WAYPOINT_DATA_DIR ?? join(process.cwd(), 'data');
const file = join(directory, 'state.json');
let state;
let writes = Promise.resolve();

export async function loadState() {
  if (state) return state;
  await mkdir(directory, { recursive: true });
  try {
    state = JSON.parse(await readFile(file, 'utf8'));
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
    state = { xp: {} };
  }
  state.xp ??= {};
  return state;
}

export async function saveState() {
  const snapshot = JSON.stringify(await loadState(), null, 2);
  writes = writes.then(async () => {
    const temporary = `${file}.${process.pid}.tmp`;
    await writeFile(temporary, snapshot, { mode: 0o600 });
    await rename(temporary, file);
  });
  return writes;
}
