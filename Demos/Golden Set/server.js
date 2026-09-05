/**
 * Servidor HTTP ultraligero en Node.js nativo (sin dependencias npm).
 * Sirve la demo interactiva del Golden Set y Calibrador LLM en 5 Dimensiones.
 * 
 * Uso:
 *   node server.js [puerto]
 *   Ejemplo: node server.js 3001
 */

const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || parseInt(process.argv[2], 10) || 3001;
const BASE_DIR = __dirname;

const MIME_TYPES = {
    '.html': 'text/html; charset=utf-8',
    '.js': 'text/javascript; charset=utf-8',
    '.css': 'text/css; charset=utf-8',
    '.json': 'application/json; charset=utf-8',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.gif': 'image/gif',
    '.svg': 'image/svg+xml',
    '.ico': 'image/x-icon',
    '.woff': 'font/woff',
    '.woff2': 'font/woff2',
    '.ttf': 'font/ttf'
};

const server = http.createServer((req, res) => {
    // Manejo de CORS para desarrollo
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, x-goog-api-key');

    if (req.method === 'OPTIONS') {
        res.writeHead(204);
        res.end();
        return;
    }

    let reqUrl = req.url.split('?')[0];
    let reqPath = decodeURI(reqUrl);

    // API Healthcheck
    if (reqPath === '/api/health') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ status: 'ok', uptime: process.uptime(), version: '1.0.0' }));
        return;
    }

    // Helper para realizar peticiones HTTPS / HTTP
    function forwardHttpRequest(targetUrl, options, postData) {
        return new Promise((resolve, reject) => {
            const parsedUrl = new URL(targetUrl);
            const isHttps = parsedUrl.protocol === 'https:';
            const client = isHttps ? https : http;

            const reqOpts = {
                protocol: parsedUrl.protocol,
                hostname: parsedUrl.hostname,
                port: parsedUrl.port || (isHttps ? 443 : 80),
                path: parsedUrl.pathname + parsedUrl.search,
                method: options.method || 'GET',
                headers: options.headers || {}
            };

            const clientReq = client.request(reqOpts, (clientRes) => {
                let data = '';
                clientRes.on('data', chunk => { data += chunk; });
                clientRes.on('end', () => {
                    resolve({
                        statusCode: clientRes.statusCode,
                        headers: clientRes.headers,
                        data: data
                    });
                });
            });

            clientReq.on('error', (err) => {
                reject(err);
            });

            if (postData) {
                clientReq.write(typeof postData === 'string' ? postData : JSON.stringify(postData));
            }
            clientReq.end();
        });
    }

    // Endpoint: Listar modelos disponibles dinámicamente según Proveedor y API Key
    if (req.method === 'POST' && reqPath === '/api/models') {
        let body = '';
        req.on('data', chunk => { body += chunk; });
        req.on('end', async () => {
            try {
                const parsed = JSON.parse(body || '{}');
                const provider = parsed.provider || 'gemini';
                const apiKey = parsed.apiKey || '';
                const customBaseUrl = parsed.baseUrl || '';

                let models = [];

                if (provider === 'gemini') {
                    if (!apiKey) throw new Error('API Key requerida para consultar modelos de Gemini.');
                    const url = `https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey}`;
                    const resp = await forwardHttpRequest(url, { method: 'GET' });
                    if (resp.statusCode >= 400) {
                        res.writeHead(resp.statusCode, { 'Content-Type': 'application/json' });
                        res.end(resp.data);
                        return;
                    }
                    const data = JSON.parse(resp.data);
                    const list = data.models || [];
                    models = list
                        .filter(m => (m.supportedGenerationMethods || []).includes('generateContent') || m.name.includes('gemini'))
                        .map(m => {
                            const cleanId = m.name.replace(/^models\//, '');
                            return {
                                id: cleanId,
                                name: m.displayName || cleanId,
                                description: m.description || '',
                                isRecommended: cleanId.includes('2.5-flash') || cleanId.includes('1.5-flash')
                            };
                        });
                } else if (provider === 'openai') {
                    if (!apiKey) throw new Error('API Key requerida para consultar modelos de OpenAI.');
                    const url = 'https://api.openai.com/v1/models';
                    const resp = await forwardHttpRequest(url, {
                        method: 'GET',
                        headers: { 'Authorization': `Bearer ${apiKey}` }
                    });
                    if (resp.statusCode >= 400) {
                        res.writeHead(resp.statusCode, { 'Content-Type': 'application/json' });
                        res.end(resp.data);
                        return;
                    }
                    const data = JSON.parse(resp.data);
                    const list = data.data || [];
                    models = list
                        .filter(m => m.id.startsWith('gpt-') || m.id.startsWith('o1') || m.id.startsWith('o3') || m.id.startsWith('chatgpt'))
                        .sort((a, b) => (b.created || 0) - (a.created || 0))
                        .map(m => ({
                            id: m.id,
                            name: m.id,
                            description: `OpenAI (${m.owned_by || 'system'})`,
                            isRecommended: m.id === 'gpt-4o' || m.id === 'gpt-4o-mini'
                        }));
                } else if (provider === 'groq') {
                    if (!apiKey) throw new Error('API Key requerida para consultar modelos de Groq.');
                    const url = 'https://api.groq.com/openai/v1/models';
                    const resp = await forwardHttpRequest(url, {
                        method: 'GET',
                        headers: { 'Authorization': `Bearer ${apiKey}` }
                    });
                    if (resp.statusCode >= 400) {
                        res.writeHead(resp.statusCode, { 'Content-Type': 'application/json' });
                        res.end(resp.data);
                        return;
                    }
                    const data = JSON.parse(resp.data);
                    const list = data.data || [];
                    models = list
                        .filter(m => m.active !== false && !m.id.includes('whisper') && !m.id.includes('guard'))
                        .map(m => ({
                            id: m.id,
                            name: m.id,
                            description: `Groq (${m.owned_by || 'groq'})`,
                            isRecommended: m.id.includes('llama-3.3-70b') || m.id.includes('llama3-70b') || m.id.includes('mixtral')
                        }));
                } else if (provider === 'openrouter') {
                    if (!apiKey) throw new Error('API Key requerida para consultar modelos de OpenRouter.');
                    const url = 'https://openrouter.ai/api/v1/models';
                    const resp = await forwardHttpRequest(url, {
                        method: 'GET',
                        headers: { 'Authorization': `Bearer ${apiKey}` }
                    });
                    if (resp.statusCode >= 400) {
                        res.writeHead(resp.statusCode, { 'Content-Type': 'application/json' });
                        res.end(resp.data);
                        return;
                    }
                    const data = JSON.parse(resp.data);
                    const list = data.data || [];
                    models = list.slice(0, 80).map(m => ({
                        id: m.id,
                        name: m.name || m.id,
                        description: m.description ? m.description.slice(0, 100) + '...' : `OpenRouter (${m.id})`,
                        isRecommended: m.id.includes('gemini-2.5') || m.id.includes('claude-3-5') || m.id.includes('gpt-4o')
                    }));
                } else if (provider === 'anthropic') {
                    if (!apiKey) throw new Error('API Key requerida para consultar modelos de Anthropic.');
                    // Anthropic API /v1/models
                    const url = 'https://api.anthropic.com/v1/models';
                    try {
                        const resp = await forwardHttpRequest(url, {
                            method: 'GET',
                            headers: {
                                'x-api-key': apiKey,
                                'anthropic-version': '2023-06-01'
                            }
                        });
                        if (resp.statusCode === 200) {
                            const data = JSON.parse(resp.data);
                            models = (data.data || []).map(m => ({
                                id: m.id,
                                name: m.display_name || m.id,
                                description: `Anthropic Claude`,
                                isRecommended: m.id.includes('sonnet')
                            }));
                        } else {
                            throw new Error('Fallback to default list');
                        }
                    } catch (e) {
                        // Fallback con modelos oficiales activos
                        models = [
                            { id: 'claude-3-7-sonnet-latest', name: 'Claude 3.7 Sonnet (Latest)', description: 'Anthropic Claude', isRecommended: true },
                            { id: 'claude-3-5-sonnet-latest', name: 'Claude 3.5 Sonnet (Latest)', description: 'Anthropic Claude', isRecommended: true },
                            { id: 'claude-3-5-haiku-latest', name: 'Claude 3.5 Haiku (Latest)', description: 'Anthropic Claude', isRecommended: false },
                            { id: 'claude-3-opus-latest', name: 'Claude 3 Opus (Latest)', description: 'Anthropic Claude', isRecommended: false }
                        ];
                    }
                } else if (provider === 'ollama' || provider === 'custom') {
                    const baseUrl = (customBaseUrl || 'http://localhost:11434').replace(/\/+$/, '');
                    const url = baseUrl.includes('/v1') ? `${baseUrl}/models` : `${baseUrl}/v1/models`;
                    try {
                        const resp = await forwardHttpRequest(url, {
                            method: 'GET',
                            headers: apiKey ? { 'Authorization': `Bearer ${apiKey}` } : {}
                        });
                        if (resp.statusCode === 200) {
                            const data = JSON.parse(resp.data);
                            const list = data.data || data.models || [];
                            models = list.map(m => ({
                                id: m.id || m.name,
                                name: m.id || m.name,
                                description: `Local (${provider})`,
                                isRecommended: true
                            }));
                        } else {
                            throw new Error(`HTTP ${resp.statusCode}`);
                        }
                    } catch (e) {
                        models = [
                            { id: 'llama3:latest', name: 'llama3:latest', description: 'Local (Ollama)', isRecommended: true },
                            { id: 'qwen2.5-coder:latest', name: 'qwen2.5-coder:latest', description: 'Local (Ollama)', isRecommended: true },
                            { id: 'mistral:latest', name: 'mistral:latest', description: 'Local (Ollama)', isRecommended: false }
                        ];
                    }
                } else {
                    throw new Error(`Proveedor no soportado: ${provider}`);
                }

                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ ok: true, provider, count: models.length, models }));

            } catch (err) {
                res.writeHead(400, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ ok: false, error: err.message || 'Error desconocido' }));
            }
        });
        return;
    }

    // Endpoint: Proxy universal de Evaluación e Inferencia LLM
    if (req.method === 'POST' && reqPath === '/api/evaluate') {
        let body = '';
        req.on('data', chunk => { body += chunk; });
        req.on('end', async () => {
            try {
                const parsed = JSON.parse(body || '{}');
                const provider = parsed.provider || 'gemini';
                const apiKey = parsed.apiKey || req.headers['x-goog-api-key'] || process.env.GEMINI_API_KEY;
                const model = parsed.model || 'gemini-2.5-flash';
                const systemPrompt = parsed.systemPrompt || '';
                const userPrompt = parsed.userPrompt || '';
                const customBaseUrl = parsed.baseUrl || '';

                if (!apiKey && provider !== 'ollama') {
                    res.writeHead(400, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify({ error: `API Key no provista para ${provider}.` }));
                    return;
                }

                if (provider === 'gemini') {
                    const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;
                    const payload = parsed.geminiPayload || {
                        contents: [{
                            role: 'user',
                            parts: [{ text: `${systemPrompt}\n\n── ENTRADA DEL CASO ──\n\n${userPrompt}` }]
                        }],
                        generationConfig: {
                            temperature: 0.0,
                            topP: 0.0,
                            responseMimeType: 'application/json'
                        }
                    };

                    const resp = await forwardHttpRequest(geminiUrl, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' }
                    }, payload);

                    res.writeHead(resp.statusCode, { 'Content-Type': 'application/json' });
                    res.end(resp.data);
                    return;

                } else if (provider === 'openai' || provider === 'groq' || provider === 'openrouter' || provider === 'ollama' || provider === 'custom') {
                    let endpointUrl = '';
                    let authHeader = apiKey ? `Bearer ${apiKey}` : undefined;

                    if (provider === 'openai') endpointUrl = 'https://api.openai.com/v1/chat/completions';
                    else if (provider === 'groq') endpointUrl = 'https://api.groq.com/openai/v1/chat/completions';
                    else if (provider === 'openrouter') endpointUrl = 'https://openrouter.ai/api/v1/chat/completions';
                    else {
                        const base = (customBaseUrl || 'http://localhost:11434').replace(/\/+$/, '');
                        endpointUrl = base.includes('/v1') ? `${base}/chat/completions` : `${base}/v1/chat/completions`;
                    }

                    const payload = {
                        model: model,
                        messages: [
                            { role: 'system', content: systemPrompt },
                            { role: 'user', content: userPrompt }
                        ],
                        temperature: 0.0,
                        response_format: { type: 'json_object' }
                    };

                    const headers = { 'Content-Type': 'application/json' };
                    if (authHeader) headers['Authorization'] = authHeader;
                    if (provider === 'openrouter') {
                        headers['HTTP-Referer'] = 'http://localhost:3001';
                        headers['X-Title'] = 'UTN FRC Golden Set';
                    }

                    const resp = await forwardHttpRequest(endpointUrl, {
                        method: 'POST',
                        headers: headers
                    }, payload);

                    if (resp.statusCode >= 400) {
                        res.writeHead(resp.statusCode, { 'Content-Type': 'application/json' });
                        res.end(resp.data);
                        return;
                    }

                    const data = JSON.parse(resp.data);
                    const contentText = data.choices?.[0]?.message?.content || '';

                    // Normalizar respuesta como textResponse para el frontend
                    res.writeHead(200, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify({
                        provider,
                        model,
                        textResponse: contentText,
                        raw: data
                    }));
                    return;

                } else if (provider === 'anthropic') {
                    const endpointUrl = 'https://api.anthropic.com/v1/messages';
                    const payload = {
                        model: model,
                        max_tokens: 2048,
                        system: systemPrompt,
                        messages: [
                            { role: 'user', content: userPrompt }
                        ],
                        temperature: 0.0
                    };

                    const resp = await forwardHttpRequest(endpointUrl, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'x-api-key': apiKey,
                            'anthropic-version': '2023-06-01'
                        }
                    }, payload);

                    if (resp.statusCode >= 400) {
                        res.writeHead(resp.statusCode, { 'Content-Type': 'application/json' });
                        res.end(resp.data);
                        return;
                    }

                    const data = JSON.parse(resp.data);
                    const contentText = (data.content || []).map(c => c.text || '').join('\n');

                    res.writeHead(200, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify({
                        provider,
                        model,
                        textResponse: contentText,
                        raw: data
                    }));
                    return;

                } else {
                    throw new Error(`Proveedor desconocido: ${provider}`);
                }

            } catch (err) {
                res.writeHead(500, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: `Error en proxy de evaluación: ${err.message}` }));
            }
        });
        return;
    }

    // Servir Archivos Estáticos
    if (reqPath === '/' || reqPath === '') {
        reqPath = '/index.html';
    }

    const safePath = path.normalize(path.join(BASE_DIR, reqPath));

    if (!safePath.startsWith(BASE_DIR)) {
        res.writeHead(403, { 'Content-Type': 'text/plain; charset=utf-8' });
        res.end('403 Prohibido: Acceso fuera del directorio.');
        return;
    }

    fs.stat(safePath, (err, stats) => {
        if (err || !stats.isFile()) {
            res.writeHead(404, { 'Content-Type': 'text/html; charset=utf-8' });
            res.end(`
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <title>404 No Encontrado</title>
                    <style>
                        body { background: #080c16; color: #f8fafc; font-family: monospace; padding: 40px; text-align: center; }
                        h1 { color: #f43f5e; }
                        a { color: #38bdf8; text-decoration: none; border-bottom: 1px solid #38bdf8; }
                    </style>
                </head>
                <body>
                    <h1>404 — Archivo no encontrado</h1>
                    <p>La ruta solicitada <code>${reqPath}</code> no existe.</p>
                    <p><a href="/">← Volver al inicio</a></p>
                </body>
                </html>
            `);
            return;
        }

        const ext = path.extname(safePath).toLowerCase();
        const contentType = MIME_TYPES[ext] || 'application/octet-stream';

        res.writeHead(200, {
            'Content-Type': contentType,
            'Content-Length': stats.size,
            'Cache-Control': 'no-cache'
        });

        const readStream = fs.createReadStream(safePath);
        readStream.pipe(res);
    });
});

server.listen(PORT, () => {
    console.log('');
    console.log('  \x1b[36m╔════════════════════════════════════════════════════════════════╗\x1b[0m');
    console.log('  \x1b[36m║\x1b[0m   \x1b[1m\x1b[32m🌟 DEMO INTERACTIVA: GOLDEN SET & CALIBRACIÓN LLM (5D)\x1b[0m      \x1b[36m║\x1b[0m');
    console.log('  \x1b[36m║\x1b[0m   \x1b[90mUTN FRC — Cátedra de Programación III — Trabajo Integrador\x1b[0m   \x1b[36m║\x1b[0m');
    console.log('  \x1b[36m╠════════════════════════════════════════════════════════════════╣\x1b[0m');
    console.log(`  \x1b[36m║\x1b[0m   Servidor local activo en: \x1b[1m\x1b[33mhttp://localhost:${PORT}\x1b[0m                \x1b[36m║\x1b[0m`);
    console.log('  \x1b[36m║\x1b[0m   Modo: Servidor estático + Proxy API Gemini                   \x1b[36m║\x1b[0m');
    console.log('  \x1b[36m║\x1b[0m   Presiona \x1b[31mCtrl+C\x1b[0m para detener el servidor                      \x1b[36m║\x1b[0m');
    console.log('  \x1b[36m╚════════════════════════════════════════════════════════════════╝\x1b[0m');
    console.log('');
});
