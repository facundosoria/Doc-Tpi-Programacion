package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;


import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;

class LegacyGoldenSetRouteAbsentTest {
  private final MockMvc mvc = MockMvcBuilders.standaloneSetup().build();

  @Test void everyLegacyGoldenSetMethodIsUnmapped() throws Exception {
    mvc.perform(get("/api/llm/golden-sets")).andExpect(status().isNotFound());
    mvc.perform(post("/api/llm/golden-sets")).andExpect(status().isNotFound());
    mvc.perform(patch("/api/llm/golden-sets/00000000-0000-0000-0000-000000000001")).andExpect(status().isNotFound());
    mvc.perform(delete("/api/llm/golden-sets/00000000-0000-0000-0000-000000000001")).andExpect(status().isNotFound());
  }
}
