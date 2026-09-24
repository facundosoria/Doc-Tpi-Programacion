package ar.edu.utn.frc.tup.piv.llm.moderation.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador web liviano para la interfaz docente de supervisión y resolución de incidentes (LLM-S12-H02 / Requisito 3).
 * Retorna HTML interactivo responsivo fiel al prototipo de la ficha de HU.
 */
@RestController
public class ModerationTeacherViewController {

    @GetMapping(value = {"/moderation/ui/incidents", "${app.api.private-path:/api/llm}/moderation/ui/incidents"}, produces = MediaType.TEXT_HTML_VALUE)
    public String getInboxView(@RequestParam(name = "course_id", defaultValue = "curso-42") String courseId) {
        String safeCourse = (courseId != null && !courseId.isBlank()) ? courseId.trim() : "curso-42";
        return HTML_TEMPLATE.replace("{{COURSE_ID}}", safeCourse);
    }

    private static final String HTML_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Supervisión Docente — Moderación de Chat</title>
              <style>
                :root {
                  --primary: #1976d2;
                  --primary-dark: #1565c0;
                  --danger: #d32f2f;
                  --success: #2e7d32;
                  --warning: #ed6c02;
                  --bg: #f8f9fa;
                  --surface: #ffffff;
                  --border: #e0e0e0;
                  --text: #212121;
                  --text-muted: #666666;
                }
                * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
                body { background: var(--bg); color: var(--text); padding: 24px; }
                .container { max-width: 1080px; margin: 0 auto; }
                header { background: var(--surface); padding: 20px 24px; border-radius: 8px; border: 1px solid var(--border); margin-bottom: 24px; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
                h1 { font-size: 1.25rem; display: flex; align-items: center; gap: 8px; }
                .course-tag { background: #e3f2fd; color: var(--primary-dark); padding: 4px 10px; border-radius: 16px; font-size: 0.85rem; font-weight: 600; }
                .filters { display: flex; gap: 12px; margin-bottom: 16px; align-items: center; }
                select, input { padding: 8px 12px; border: 1px solid var(--border); border-radius: 6px; font-size: 0.9rem; }
                .btn { padding: 8px 16px; border: none; border-radius: 6px; font-weight: 600; cursor: pointer; transition: background 0.2s; }
                .btn-primary { background: var(--primary); color: white; }
                .btn-primary:hover { background: var(--primary-dark); }
                .btn-secondary { background: #e0e0e0; color: var(--text); }
                .btn-sm { padding: 6px 12px; font-size: 0.85rem; }
                .card { background: var(--surface); border: 1px solid var(--border); border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); overflow: hidden; }
                table { width: 100%; border-collapse: collapse; text-align: left; }
                th, td { padding: 14px 16px; border-bottom: 1px solid var(--border); font-size: 0.9rem; }
                th { background: #fafafa; font-weight: 600; color: var(--text-muted); text-transform: uppercase; font-size: 0.75rem; letter-spacing: 0.5px; }
                tr:hover { background: #fdfdfd; }
                .preview-cell { max-width: 380px; word-break: break-word; color: #37474f; font-family: monospace; font-size: 0.85rem; }
                .badge { display: inline-block; padding: 3px 8px; border-radius: 4px; font-size: 0.75rem; font-weight: 600; }
                .badge-danger { background: #ffebee; color: var(--danger); }
                .badge-success { background: #e8f5e9; color: var(--success); }
                .badge-warning { background: #fff3e0; color: var(--warning); }
                .modal { display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.5); align-items: center; justify-content: center; z-index: 100; }
                .modal.active { display: flex; }
                .modal-content { background: var(--surface); width: 100%; max-width: 600px; border-radius: 8px; padding: 24px; box-shadow: 0 8px 24px rgba(0,0,0,0.15); }
                .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
                .field { margin-bottom: 16px; }
                .field label { display: block; font-size: 0.85rem; font-weight: 600; color: var(--text-muted); margin-bottom: 6px; }
                .field-value { background: #f5f5f5; padding: 10px 12px; border-radius: 6px; font-size: 0.9rem; word-break: break-word; }
                textarea { width: 100%; height: 90px; padding: 10px; border: 1px solid var(--border); border-radius: 6px; font-size: 0.9rem; resize: vertical; }
                .char-count { font-size: 0.75rem; color: var(--text-muted); text-align: right; margin-top: 4px; }
                .radio-group { display: flex; gap: 24px; margin: 8px 0; }
                .radio-option { display: flex; align-items: center; gap: 8px; cursor: pointer; font-weight: 500; font-size: 0.95rem; }
                .modal-footer { display: flex; justify-content: flex-end; gap: 12px; margin-top: 20px; }
                .toast { position: fixed; bottom: 24px; right: 24px; padding: 12px 20px; border-radius: 6px; color: white; font-weight: 500; display: none; z-index: 200; box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
                .toast-success { background: var(--success); }
                .toast-error { background: var(--danger); }
              </style>
            </head>
            <body>
              <div class="container">
                <header>
                  <h1>🛡 Moderación de Mensajes</h1>
                  <span class="course-tag" id="courseLabel">Curso: {{COURSE_ID}}</span>
                </header>

                <div class="filters">
                  <select id="statusFilter" onchange="loadIncidents()">
                    <option value="PENDING_REVIEW">Pendientes de revisión</option>
                    <option value="CONFIRMED">Confirmados</option>
                    <option value="REVERSED">Revertidos (Falsos Positivos)</option>
                  </select>
                  <button class="btn btn-secondary" onclick="loadIncidents()">Actualizar</button>
                </div>

                <div class="card">
                  <table>
                    <thead>
                      <tr>
                        <th>Incidente</th>
                        <th>Mensaje Bloqueado (Preview máx 200 chars)</th>
                        <th>Motivo Sistema</th>
                        <th>Apelación</th>
                        <th>Acción</th>
                      </tr>
                    </thead>
                    <tbody id="incidentsTableBody">
                      <tr><td colspan="5" style="text-align:center; color: var(--text-muted);">Cargando incidentes...</td></tr>
                    </tbody>
                  </table>
                </div>
              </div>

              <!-- Modal de Resolución -->
              <div class="modal" id="resolveModal">
                <div class="modal-content">
                  <div class="modal-header">
                    <h2 style="font-size: 1.15rem;" id="modalTitle">Resolver Incidente</h2>
                    <button style="border:none;background:none;font-size:1.2rem;cursor:pointer;" onclick="closeModal()">✕</button>
                  </div>
                  
                  <div class="field">
                    <label>Mensaje bloqueado (Preview seguro):</label>
                    <div class="field-value" id="modalPreview">...</div>
                  </div>

                  <div class="field" id="modalAppealContainer" style="display:none;">
                    <label>Apelación del alumno:</label>
                    <div class="field-value" style="background:#fff8e1; border-left: 4px solid #ffb300;" id="modalAppealReason">...</div>
                  </div>

                  <div class="field">
                    <label>Resolución:</label>
                    <div class="radio-group">
                      <label class="radio-option">
                        <input type="radio" name="resolutionType" value="CONFIRMED" checked>
                        <span>Confirmar bloqueo</span>
                      </label>
                      <label class="radio-option">
                        <input type="radio" name="resolutionType" value="REVERSED">
                        <span>Revertir bloqueo (Falso positivo)</span>
                      </label>
                    </div>
                  </div>

                  <div class="field">
                    <label for="resolutionReason">Motivo de la resolución (mín. 20 caracteres):</label>
                    <textarea id="resolutionReason" placeholder="Explique el criterio docente para confirmar o revertir la moderación..." oninput="updateCharCount()"></textarea>
                    <div class="char-count" id="charCount">0 / 500 caracteres (mín. 20)</div>
                  </div>

                  <div class="modal-footer">
                    <button class="btn btn-secondary" onclick="closeModal()">Cancelar</button>
                    <button class="btn btn-primary" id="btnSubmitResolve" onclick="submitResolution()">Resolver incidente</button>
                  </div>
                </div>
              </div>

              <div class="toast" id="toast"></div>

              <script>
                const courseId = '{{COURSE_ID}}';
                let currentIncident = null;

                async function loadIncidents() {
                  const status = document.getElementById('statusFilter').value;
                  const tbody = document.getElementById('incidentsTableBody');
                  tbody.innerHTML = '<tr><td colspan="5" style="text-align:center; color: var(--text-muted);">Cargando incidentes...</td></tr>';
                  
                  try {
                    const res = await fetch(`/moderation/v1/incidents?course_id=${encodeURIComponent(courseId)}&status=${encodeURIComponent(status)}`);
                    if (!res.ok) {
                      tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; color: var(--danger);">Error cargando incidentes (${res.status})</td></tr>`;
                      return;
                    }
                    const data = await res.json();
                    if (!data.content || data.content.length === 0) {
                      tbody.innerHTML = '<tr><td colspan="5" style="text-align:center; color: var(--text-muted);">No hay incidentes para los filtros seleccionados.</td></tr>';
                      return;
                    }
                    tbody.innerHTML = data.content.map(inc => `
                      <tr>
                        <td><strong>${inc.incident_id.substring(0,8)}...</strong></td>
                        <td class="preview-cell">${escapeHtml(inc.message_preview || '(sin preview)')}</td>
                        <td><span class="badge badge-warning">${escapeHtml(inc.reason_code || 'N/A')}</span></td>
                        <td>${inc.has_appeal ? '<span class="badge badge-danger">Apelado</span>' : '<span style="color:var(--text-muted);">-</span>'}</td>
                        <td>
                          ${inc.status === 'CONFIRMED' || inc.status === 'REVERSED' 
                            ? `<span class="badge ${inc.status === 'CONFIRMED' ? 'badge-danger' : 'badge-success'}">${inc.status}</span>`
                            : `<button class="btn btn-primary btn-sm" onclick='openResolveModal(${JSON.stringify(inc)})'>Ver / Resolver</button>`
                          }
                        </td>
                      </tr>
                    `).join('');
                  } catch (e) {
                    tbody.innerHTML = '<tr><td colspan="5" style="text-align:center; color: var(--danger);">Error de conexión</td></tr>';
                  }
                }

                function openResolveModal(incident) {
                  currentIncident = incident;
                  document.getElementById('modalTitle').innerText = `Resolver Incidente ${incident.incident_id.substring(0,8)}...`;
                  document.getElementById('modalPreview').innerText = incident.message_preview || '(sin preview)';
                  
                  const appealBox = document.getElementById('modalAppealContainer');
                  if (incident.has_appeal && incident.appeal_reason) {
                    appealBox.style.display = 'block';
                    document.getElementById('modalAppealReason').innerText = incident.appeal_reason;
                  } else {
                    appealBox.style.display = 'none';
                  }
                  
                  document.getElementById('resolutionReason').value = '';
                  updateCharCount();
                  document.getElementById('resolveModal').classList.add('active');
                }

                function closeModal() {
                  document.getElementById('resolveModal').classList.remove('active');
                  currentIncident = null;
                }

                function updateCharCount() {
                  const val = document.getElementById('resolutionReason').value.trim();
                  const counter = document.getElementById('charCount');
                  counter.innerText = `${val.length} / 500 caracteres (mín. 20)`;
                  if (val.length < 20) {
                    counter.style.color = 'var(--danger)';
                  } else if (val.length > 500) {
                    counter.style.color = 'var(--danger)';
                  } else {
                    counter.style.color = 'var(--success)';
                  }
                }

                async function submitResolution() {
                  if (!currentIncident) return;
                  const reason = document.getElementById('resolutionReason').value.trim();
                  if (reason.length < 20 || reason.length > 500) {
                    showToast('El motivo debe tener entre 20 y 500 caracteres.', 'error');
                    return;
                  }
                  const resolution = document.querySelector('input[name="resolutionType"]:checked').value;
                  const btn = document.getElementById('btnSubmitResolve');
                  btn.disabled = true;
                  btn.innerText = 'Enviando...';

                  try {
                    const res = await fetch(`/moderation/v1/incidents/${currentIncident.incident_id}/resolve`, {
                      method: 'POST',
                      headers: { 'Content-Type': 'application/json' },
                      body: JSON.stringify({ resolution, resolution_reason: reason })
                    });
                    if (res.ok) {
                      showToast(`Incidente resuelto como ${resolution}`, 'success');
                      closeModal();
                      loadIncidents();
                    } else {
                      const err = await res.json().catch(() => ({}));
                      showToast(err.detail || 'Error al resolver incidente', 'error');
                    }
                  } catch (e) {
                    showToast('Error de comunicación con el servidor', 'error');
                  } finally {
                    btn.disabled = false;
                    btn.innerText = 'Resolver incidente';
                  }
                }

                function showToast(msg, type) {
                  const t = document.getElementById('toast');
                  t.className = `toast toast-${type}`;
                  t.innerText = msg;
                  t.style.display = 'block';
                  setTimeout(() => { t.style.display = 'none'; }, 4000);
                }

                function escapeHtml(text) {
                  const div = document.createElement('div');
                  div.innerText = text;
                  return div.innerHTML;
                }

                loadIncidents();
              </script>
            </body>
            </html>
            """;
}
