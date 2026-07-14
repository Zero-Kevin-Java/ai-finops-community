export function buildSimpleRouteModelQuery(originalModel: string) {
  return {
    url: '/api/simple-route/model',
    params: { originalModel }
  };
}
