import { createServer } from 'node:http';

const teacherId = '11111111-1111-1111-1111-111111111111';
const courses = [
  { id: '00000000-0000-0000-0000-000000000010', name: 'Programación III', subtitle: 'Comisión A · 2026' },
  { id: '00000000-0000-0000-0000-000000000020', name: 'Paradigmas de Programación', subtitle: 'Comisión B · 2026' },
];

function json(response, status, body) {
  response.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' });
  response.end(JSON.stringify(body));
}

/**
 * Local stand-in for external Gateway + courses-service only. It has no Tema 07 business logic:
 * every /api/llm request still reaches the real backend through Angular's development proxy.
 */
createServer((request, response) => {
  const url = new URL(request.url ?? '/', 'http://localhost:4300');
  if (request.method === 'GET' && url.pathname === '/api/courses/me/course-cohorts') {
    return json(response, 200, { items: courses });
  }

  const membership = url.pathname.match(/^\/api\/courses\/([^/]+)\/members\/([^/]+)$/);
  if (request.method === 'GET' && membership) {
    const [, courseId, userId] = membership;
    if (request.headers.authorization !== 'Bearer local-gateway-m2m-token') {
      return json(response, 401, { detail: 'M2M credential missing or invalid' });
    }
    const isTeacher = userId === teacherId && courses.some((course) => course.id === courseId);
    return isTeacher
      ? json(response, 200, { role: 'DOCENTE', status: 'ACTIVE' })
      : json(response, 404, { detail: 'Membership not found' });
  }

  return json(response, 404, { detail: 'External mock route not found' });
}).listen(4300, () => {
  console.log('Local Gateway/Courses simulator listening on http://localhost:4300');
});
