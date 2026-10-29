import test from 'node:test';
import assert from 'node:assert/strict';
import { verifyPlanSharing } from './verify-plan-sharing.mjs';

function mockFetch({ complete = true, status = 200 } = {}) {
  const calls = [];
  const fetch = async (url, options) => {
    calls.push({ url, options });
    if (url.endsWith('/models')) {
      return new Response(JSON.stringify({ models: [{ slug: 'test-model', visibility: 'list' }] }), { status: 200 });
    }
    if (status !== 200) return new Response('denied', { status });
    const events = [
      'data: {"type":"response.output_text.delta","delta":"Mine Builder connesso."}\n\n',
      complete ? 'data: {"type":"response.completed"}\n\n' : ''
    ].join('');
    return new Response(events, { status: 200, headers: { 'Content-Type': 'text/event-stream' } });
  };
  return { fetch, calls };
}

test('requires OAuth token before any network request', async () => {
  await assert.rejects(() => verifyPlanSharing(''), /OAuth token missing/);
});
test('accepts only completed streaming inference and disables storage', async () => {
  const { fetch, calls } = mockFetch();
  const result = await verifyPlanSharing('LOCAL_TEST_TOKEN', fetch);
  assert.equal(result.response, 'Mine Builder connesso.');
  assert.equal(result.success, true);
  const body = JSON.parse(calls[1].options.body);
  assert.equal(body.store, false);
  assert.equal(body.stream, true);
  assert.equal(calls[1].options.headers.Authorization, 'Bearer LOCAL_TEST_TOKEN');
});
test('fails closed if response.completed is missing', async () => {
  const { fetch } = mockFetch({ complete: false });
  await assert.rejects(() => verifyPlanSharing('LOCAL_TEST_TOKEN', fetch), /without response.completed/);
});
test('fails closed on rejected inference', async () => {
  const { fetch } = mockFetch({ status: 403 });
  await assert.rejects(() => verifyPlanSharing('LOCAL_TEST_TOKEN', fetch), /HTTP 403/);
});
