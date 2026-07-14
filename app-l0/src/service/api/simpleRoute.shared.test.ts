import assert from 'node:assert/strict';
import { buildSimpleRouteModelQuery } from './simpleRoute.shared';

assert.deepEqual(buildSimpleRouteModelQuery('openai/gpt-5.4'), {
  url: '/api/simple-route/model',
  params: {
    originalModel: 'openai/gpt-5.4'
  }
});
