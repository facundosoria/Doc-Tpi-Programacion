package ar.edu.utn.frc.tup.piv.llm.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * DEC-08 — Este servicio NO valida el JWT. Su Authentication sale
 * EXCLUSIVAMENTE de los headers X-* que inyecta el Gateway.
 * Lo que los hace confiables es que el puerto NO se publica (sin `ports:`).
 */
@Component
public class GatewayIdentityFilter extends OncePerRequestFilter {

    @Value("${llm.workbench.enabled:false}")
    private boolean workbench;

    @Value("${llm.workbench.user-id:11111111-1111-1111-1111-111111111111}")
    private String workbenchUser;

    /**
     * El payload del Bearer se lee SIN verificar la firma (el JWT lo valida el Gateway). Por eso
     * queda apagado por defecto: solo debe activarse en desarrollo/tests, nunca en despliegues
     * donde el puerto del servicio sea alcanzable sin pasar por el Gateway.
     */
    @Value("${app.security.trust-unsigned-bearer:false}")
    private boolean trustUnsignedBearer;

    public void setTrustUnsignedBearer(boolean trustUnsignedBearer) {
        this.trustUnsignedBearer = trustUnsignedBearer;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req,
                                    @NonNull HttpServletResponse res,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String tipo = req.getHeader(IdentityHeaders.PRINCIPAL_TYPE);

        String authHeader = req.getHeader("Authorization");

        if ("user".equals(tipo)) {
            autenticar(req.getHeader(IdentityHeaders.USER_ID),
                    rolesDe(req.getHeader(IdentityHeaders.USER_ROLES)));
        } else if ("service".equals(tipo)) {
            autenticar(req.getHeader(IdentityHeaders.SERVICE_ID),
                    scopesDe(req.getHeader(IdentityHeaders.SERVICE_SCOPES)));
        } else if (trustUnsignedBearer && authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            autenticarBearer(authHeader.substring(7).trim());
        } else if (workbench) {
            // Soporte retrocompatible para modo workbench local
            autenticar(workbenchUser, List.of(
                    new SimpleGrantedAuthority("ROLE_PROFESSOR"),
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_MS"),
                    new SimpleGrantedAuthority("llm.golden-set.manage"),
                    new SimpleGrantedAuthority("llm.rubric-template.manage"),
                    new SimpleGrantedAuthority("llm.tutor.interact"),
                    new SimpleGrantedAuthority("llm.rag.query")
            ));
        }
        // Sin headers y sin workbench -> ruta pública. No autentica y no falla.

        chain.doFilter(req, res);
    }

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    private void autenticarBearer(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        String[] parts = token.split("\\.");
        if (parts.length >= 2) {
            try {
                byte[] decoded = java.util.Base64.getUrlDecoder().decode(parts[1]);
                com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(decoded);
                String principal = node.has("sub") ? node.get("sub").asText() :
                        (node.has("service_id") ? node.get("service_id").asText() : "m2m-service");
                List<GrantedAuthority> authorities = new java.util.ArrayList<>();
                if (node.has("scope")) {
                    authorities.addAll(scopesDe(node.get("scope").asText().replace(" ", ",")));
                }
                if (node.has("scopes") && node.get("scopes").isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode s : node.get("scopes")) {
                        authorities.add(new SimpleGrantedAuthority(s.asText()));
                    }
                }
                if (node.has("roles") && node.get("roles").isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode r : node.get("roles")) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + r.asText()));
                    }
                }
                autenticar(principal, authorities);
            } catch (Exception ignored) {
                // Token no decodificable -> no autentica
            }
        }
    }

    private void autenticar(String principal, List<GrantedAuthority> authorities) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities));
    }

    private List<GrantedAuthority> rolesDe(String header) {
        return partes(header).stream()
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
    }

    /** DEC-05: MS es un ROL (-> ROLE_MS); los scopes son authorities peladas. */
    private List<GrantedAuthority> scopesDe(String header) {
        return partes(header).stream()
                .map(s -> (GrantedAuthority) new SimpleGrantedAuthority("MS".equals(s) ? "ROLE_MS" : s))
                .toList();
    }

    private List<String> partes(String header) {
        if (header == null || header.isBlank()) {
            return List.of();
        }
        return Arrays.stream(header.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
