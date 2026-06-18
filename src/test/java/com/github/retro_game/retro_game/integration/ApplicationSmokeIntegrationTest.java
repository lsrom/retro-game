package com.github.retro_game.retro_game.integration;

import com.github.retro_game.retro_game.repository.ItemDefinitionRepository;
import com.github.retro_game.retro_game.repository.UserRepository;
import com.github.retro_game.retro_game.entity.Coordinates;
import com.github.retro_game.retro_game.entity.CoordinatesKind;
import com.github.retro_game.retro_game.service.BodyCreationService;
import com.github.retro_game.retro_game.service.UserService;
import jakarta.persistence.EntityManager;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class ApplicationSmokeIntegrationTest extends IntegrationTest {
  @Autowired
  private ItemDefinitionRepository itemDefinitionRepository;
  @Autowired
  private TestRestTemplate restTemplate;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private UserService userService;
  @Autowired
  private UserDetailsService userDetailsService;
  @Autowired
  private BodyCreationService bodyCreationService;
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private EntityManager entityManager;

  @Test
  public void startsAndSeedsBuiltInCatalog() {
    assertThat(itemDefinitionRepository.count()).isEqualTo(57);
    assertThat(itemDefinitionRepository.findByKind("METAL_MINE")).isPresent();
    assertThat(itemDefinitionRepository.findByKind("BATTLESHIP")).isPresent();
    assertThat(itemDefinitionRepository.findByKind("ASTROPHYSICS")).isPresent();
  }

  @Test
  public void servesSignInPage() {
    var response = restTemplate.getForEntity("/", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("Sign in - Retro Game");
  }

  @Test
  @Transactional
  public void servesSettingsPage() throws Exception {
    var email = "settings-smoke@test";
    var userId = userService.create(email, "settingsSmoke", "test");
    var account = userRepository.findById(userId).orElseThrow();
    var coordinates = new Coordinates();
    coordinates.setGalaxy(1);
    coordinates.setSystem(1);
    coordinates.setPosition(1);
    coordinates.setKind(CoordinatesKind.PLANET);
    var body = bodyCreationService.createColony(account, coordinates, Date.from(Instant.now()));
    entityManager.flush();
    entityManager.clear();
    var userDetails = userDetailsService.loadUserByUsername(email);
    var session = new MockHttpSession();

    mockMvc.perform(get("/settings")
            .param("body", Long.toString(body.getId()))
            .session(session)
            .with(user(userDetails))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Settings")));
  }
}
