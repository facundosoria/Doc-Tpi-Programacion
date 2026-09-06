/**
 * Servidor HTTP ultraligero en Node.js nativo (sin dependencias npm).
 * Sirve la demo interactiva de Seguridad y Defensa en Profundidad para LLMs.
 * 
 * Uso:
 *   node server.js [puerto]
 *   Ejemplo: node server.js 3000
 */

const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || parseInt(process.argv[2], 10) || 3000;
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
    let reqPath = decodeURI(req.url.split('?')[0]);
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
                    <p>No se encontró: <code>${reqPath}</code></p>
                    <p><a href="/index.html">Ir al Simulador de Seguridad</a></p>
                </body>
                </html>
            `);
            return;
        }

        const ext = path.extname(safePath).toLowerCase();
        const contentType = MIME_TYPES[ext] || 'application/octet-stream';

        res.writeHead(200, {
            'Content-Type': contentType,
            'Cache-Control': 'no-cache',
            'Access-Control-Allow-Origin': '*'
        });

        const stream = fs.createReadStream(safePath);
        stream.pipe(res);
    });
});

server.listen(PORT, () => {
    console.log('\n================================================================');
    console.log('🛡️  DEMO: SIMULADOR DE SEGURIDAD LLM (8 CAPAS ANTI-JAILBREAK)');
    console.log('================================================================');
    console.log(`🌐 Acceso local:   http://localhost:${PORT}`);
    console.log(`📂 Directorio:     ${BASE_DIR}`);
    console.log('Presiona Ctrl+C para detener el servidor.\n');
});
