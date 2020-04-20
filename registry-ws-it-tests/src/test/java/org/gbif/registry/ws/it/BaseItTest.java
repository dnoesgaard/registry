package org.gbif.registry.ws.it;

import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.ws.it.fixtures.TestConstants;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Collections;

import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Base class for IT tests that initializes data sources and basic security settings.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
  classes = RegistryIntegrationTestsConfiguration.class,
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {NetworkEntityTest.ContextInitializer.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class BaseItTest {

  /**
   * Custom ContextInitializer to expose the registry DB data source and search flags.
   */
  public static class ContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues.of(dbTestPropertyPairs())
        .applyTo(configurableApplicationContext.getEnvironment());
      withSearchEnabled(false, configurableApplicationContext.getEnvironment());
    }

    protected static void withSearchEnabled(
      boolean enabled, ConfigurableEnvironment configurableEnvironment) {
      TestPropertyValues.of("searchEnabled=" + enabled).applyTo(configurableEnvironment);
    }

    protected String[] dbTestPropertyPairs() {
      return new String[] {
        "registry.datasource.url=jdbc:postgresql://localhost:"
        + database.getConnectionInfo().getPort()
        + "/"
        + database.getConnectionInfo().getDbName(),
        "registry.datasource.username=" + database.getConnectionInfo().getUser(),
        "registry.datasource.password="
      };
    }
  }

  @RegisterExtension
  static PreparedDbExtension database =
    EmbeddedPostgresExtension.preparedDatabase(
      LiquibasePreparer.forClasspathLocation(TestConstants.LIQUIBASE_MASTER_FILE));

  @RegisterExtension
  public final DatabaseInitializer databaseRule =
    new DatabaseInitializer(database.getTestDatabase());


  private final SimplePrincipalProvider simplePrincipalProvider;

  public BaseItTest(SimplePrincipalProvider simplePrincipalProvider) {
    this.simplePrincipalProvider = simplePrincipalProvider;
  }

  @BeforeEach
  public void setup() {
    // reset SimplePrincipleProvider, configured for web service client tests only
    if (simplePrincipalProvider != null) {
      simplePrincipalProvider.setPrincipal(TestConstants.TEST_ADMIN);
      SecurityContext ctx = SecurityContextHolder.createEmptyContext();
      SecurityContextHolder.setContext(ctx);
      ctx.setAuthentication(
        new UsernamePasswordAuthenticationToken(
          simplePrincipalProvider.get().getName(),
          "",
          Collections.singleton(new SimpleGrantedAuthority(UserRole.REGISTRY_ADMIN.name()))));
    }
  }

  public SimplePrincipalProvider getSimplePrincipalProvider() {
    return simplePrincipalProvider;
  }
}
