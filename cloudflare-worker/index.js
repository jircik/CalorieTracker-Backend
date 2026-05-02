const FATSECRET_AUTH_URL = 'https://oauth.fatsecret.com/connect/token';
const FATSECRET_API_URL  = 'https://platform.fatsecret.com';
const TOKEN_KV_KEY       = 'fat_token';

async function getValidToken(env) {
  const cached = await env.FATSECRET_TOKEN_KV.get(TOKEN_KV_KEY, 'json');
  if (cached && cached.expires_at > Date.now()) {
    return cached.access_token;
  }

  const credentials = btoa(`${env.FATSECRET_CLIENT_ID}:${env.FATSECRET_CLIENT_SECRET}`);
  const resp = await fetch(FATSECRET_AUTH_URL, {
    method: 'POST',
    headers: {
      'Authorization': `Basic ${credentials}`,
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: 'grant_type=client_credentials&scope=basic',
  });

  if (!resp.ok) {
    const body = await resp.text();
    throw new Error(`FatSecret auth failed (${resp.status}): ${body}`);
  }

  const data = await resp.json();
  const token = {
    access_token: data.access_token,
    expires_at: Date.now() + (data.expires_in - 60) * 1000,
  };

  await env.FATSECRET_TOKEN_KV.put(TOKEN_KV_KEY, JSON.stringify(token), {
    expirationTtl: data.expires_in - 60,
  });

  return token.access_token;
}

async function proxyToFatSecret(path, bearerToken) {
  const resp = await fetch(`${FATSECRET_API_URL}${path}`, {
    headers: { 'Authorization': `Bearer ${bearerToken}` },
  });
  const body = await resp.text();
  return new Response(body, {
    status: resp.status,
    headers: { 'Content-Type': 'application/json' },
  });
}

export default {
  async fetch(request, env) {
    if (request.headers.get('X-Proxy-Key') !== env.PROXY_SECRET) {
      return new Response('Unauthorized', { status: 401 });
    }

    const url = new URL(request.url);
    let token;

    try {
      token = await getValidToken(env);
    } catch (err) {
      return new Response(JSON.stringify({ error: err.message }), {
        status: 503,
        headers: { 'Content-Type': 'application/json' },
      });
    }

    // GET /search?q={query}
    if (url.pathname === '/search') {
      const q = url.searchParams.get('q');
      if (!q) return new Response('Missing query param: q', { status: 400 });
      const fsPath = `/rest/foods/search/v1?search_expression=${encodeURIComponent(q)}&max_results=5&format=json`;
      return proxyToFatSecret(fsPath, token);
    }

    // GET /food/{id}
    const foodMatch = url.pathname.match(/^\/food\/(.+)$/);
    if (foodMatch) {
      const foodId = foodMatch[1];
      const fsPath = `/rest/food/v5?food_id=${encodeURIComponent(foodId)}&format=json`;
      return proxyToFatSecret(fsPath, token);
    }

    return new Response('Not found', { status: 404 });
  },
};
