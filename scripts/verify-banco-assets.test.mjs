import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const readJson = path => JSON.parse(readFileSync(new URL('../' + path, import.meta.url), 'utf8'));

test('Banco IA blockstate resolves to its block model', () => {
  const state = readJson('src/main/resources/assets/minebuilder/blockstates/banco_ia.json');
  assert.equal(state.variants[''].model, 'minebuilder:block/banco_ia');
  const block = readJson('src/main/resources/assets/minebuilder/models/block/banco_ia.json');
  assert.equal(block.parent, 'minecraft:block/cube_all');
  assert.ok(block.textures.all);
});

test('Banco IA item resolves to its block model', () => {
  const item = readJson('src/main/resources/assets/minebuilder/models/item/banco_ia.json');
  assert.equal(item.parent, 'minebuilder:block/banco_ia');
});

test('Banco IA has Italian and English names', () => {
  for (const lang of ['it_it', 'en_us']) {
    const strings = readJson('src/main/resources/assets/minebuilder/lang/' + lang + '.json');
    assert.ok(strings['block.minebuilder.banco_ia']);
  }
});

test('Banco IA drops itself when broken', () => {
  const loot = readJson('src/main/resources/data/minebuilder/loot_tables/blocks/banco_ia.json');
  assert.equal(loot.type, 'minecraft:block');
  assert.ok(loot.pools.some(pool => pool.entries.some(entry => entry.name === 'minebuilder:banco_ia')));
});
