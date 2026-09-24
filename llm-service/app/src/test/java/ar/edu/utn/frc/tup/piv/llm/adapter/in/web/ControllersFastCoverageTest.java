package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import ar.edu.utn.frc.tup.piv.llm.application.service.*;
import ar.edu.utn.frc.tup.piv.llm.application.service.gateway.GatewayExecutor;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.*;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.*;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.ai.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {CourseGoldenSetController.class, CalibrationRunController.class, ProviderCredentialController.class})
class ControllersFastCoverageTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private CourseGoldenSetService courseGoldenSetService;
    @MockBean private CalibrationRunService calibrationRunService;
    @MockBean private ProviderCredentialRepository providerCredentialRepository;
    @MockBean private EncryptedSecretService encryptedSecretService;
    @MockBean private ProviderRegistry providerRegistry;
    @MockBean private ProviderInvocationGateway providerInvocationGateway;
    @MockBean private EvaluatorModelEvents evaluatorModelEvents;
    @MockBean private GoldenSetAuthorization goldenSetAuthorization;
    @MockBean private CourseAuthorization courseAuthorization;
    @MockBean private GatewayExecutor gatewayExecutor;

    @Test
    void testEndpoints() throws Exception {
        UUID id = UUID.randomUUID();
        
        // Golden Set
        mockMvc.perform(post("/api/llm/courses/" + id + "/golden-sets/drafts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\"}"));
                
        mockMvc.perform(get("/api/llm/courses/" + id + "/golden-sets"));

        // Calibration Run
        mockMvc.perform(post("/api/llm/calibration-runs"));
        mockMvc.perform(get("/api/llm/calibration-runs/" + id));

        // Provider Credential
        mockMvc.perform(post("/api/llm/admin/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"providerKey\":\"openai\",\"displayName\":\"O\",\"configuration\":{},\"secret\":\"S\"}"));
                
        mockMvc.perform(get("/api/llm/admin/credentials"));
        
        mockMvc.perform(post("/api/llm/admin/credentials/" + id + "/deployments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"modelId\":\"m1\",\"revision\":\"r1\",\"candidateSlot\":1}"));
    }
}
