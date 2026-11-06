// Install the official validator with: npm ci --prefix tools/style-validation
const fs = require('node:fs');
const path = require('node:path');
const {validateStyleMin} = require('../tools/style-validation/node_modules/@maplibre/maplibre-gl-style-spec');
const root = path.join(__dirname, '../app/src/main/assets/styles');
const files = fs.readdirSync(root).filter(n => n.endsWith('.json'));
let total = 0;
const styles = new Map();
for (const file of files) {
  const style = JSON.parse(fs.readFileSync(path.join(root, file), 'utf8'));
  styles.set(file, style);
  const errors = validateStyleMin(style);
  for (const error of errors) console.error(`${file}: ${error.message}`);
  total += errors.length;
}

function structure(style) {
  return JSON.stringify(style.layers.map(layer => [
    layer.id,
    layer.type,
    layer['source-layer'] || '',
    layer.minzoom ?? '',
    layer.maxzoom ?? '',
    layer.filter || null,
    layer.type === 'symbol' ? layer.layout : null,
  ]));
}

const dayFiles = files.filter(file => !file.endsWith('_night.json') && !file.startsWith('division_holo'));
if (dayFiles.length !== 38) {
  console.error(`Expected 38 world day styles; found ${dayFiles.length}`);
  total += 1;
}
const signatures = new Set();
for (const file of dayFiles) {
  const style = styles.get(file);
  const meta = style.metadata || {};
  for (const key of ['waypoint:land-order', 'waypoint:feature-order', 'waypoint:road-order', 'waypoint:label-priority']) {
    if (typeof meta[key] !== 'string' || !meta[key]) {
      console.error(`${file}: missing structural grammar metadata ${key}`);
      total += 1;
    }
  }
  signatures.add(structure(style));
  const nightFile = file.replace(/\.json$/, '_night.json');
  const night = styles.get(nightFile);
  if (!night || structure(style) !== structure(night)) {
    console.error(`${file}: day and night layer structures differ`);
    total += 1;
  }
}
if (signatures.size !== 38) {
  console.error(`Expected 38 structurally distinct world styles; found ${signatures.size}`);
  total += 1;
}

console.log(`${files.length} styles checked; ${total} errors; ${signatures.size} distinct world map grammars`);
process.exitCode = total ? 1 : 0;
