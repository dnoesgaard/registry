package org.gbif.identity.mybatis;

import org.gbif.api.model.common.User;
import org.gbif.api.model.common.UserCreation;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.identity.email.IdentityEmailManager;
import org.gbif.identity.email.IdentityEmailManagerMock;
import org.gbif.identity.guice.IdentityTestModule;
import org.gbif.identity.model.ModelError;
import org.gbif.identity.model.UserCreationResult;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class IdentityServiceImplIT {

  // Sets up the database schema
  @ClassRule
  public static final LiquibaseInitializer liquibaseRule = new LiquibaseInitializer(LiquibaseModules.database());

  // truncates the tables
  @Rule
  public final DatabaseInitializer databaseRule = new DatabaseInitializer(LiquibaseModules.database());

  private static final String TEST_PASSWORD = "[password]";
  private static final String TEST_PASSWORD2 = "]password[";
  private static final AtomicInteger index = new AtomicInteger(0);

  private IdentityService service;
  private IdentityEmailManagerMock emailManager;

  @Before
  public void testSetup() throws Exception {
    Properties props = PropertiesUtil.loadProperties("registry-test.properties");

    Module mod = new IdentityTestModule(props);
    Injector inj = Guice.createInjector(mod);
    service = inj.getInstance(IdentityService.class);
    emailManager = (IdentityEmailManagerMock)inj.getInstance(IdentityEmailManager.class);
  }

  /**
   * Checks the typical CRUD process with correct data only (i.e. no failure scenarios).
   */
  @Test
  public void testCRUD() throws Exception {
    UserCreation u1 = generateUser();

    // create
    UserCreationResult result = service.create(u1);
    assertNotNull("Expected the key to be set", result.getKey());

    // get
    User u2 = service.get(u1.getUserName());
    assertEquals(u1.getUserName(), u2.getUserName());
    assertEquals(u1.getFirstName(), u2.getFirstName());
    assertEquals(u1.getLastName(), u2.getLastName());
    assertEquals(2, u2.getSettings().size());
    assertEquals(1, u2.getRoles().size());
    assertNull(u2.getLastLogin());

    // update
    u2.getSettings().put("user.country", "GB");
    u2.getRoles().add(UserRole.REGISTRY_ADMIN);
    service.update(u2);

    User u3 = service.get(u1.getUserName());
    assertEquals(2, u3.getSettings().size());
    assertEquals("GB", u3.getSettings().get("user.country"));
    assertEquals(2, u3.getRoles().size());

    service.delete(u1.getUserName());

    User u4 = service.get(u1.getUserName());
    assertNull(u4);
  }

  /**
   * Checks the typical CRUD process with correct data only (i.e. no failure scenarios).
   */
  @Test
  public void testCreateError() throws Exception {
    UserCreation u1 = generateUser();
    // create
    UserCreationResult result = service.create(u1);
    assertNotNull("Expected the key to be set", result.getKey());

    // try to create it again with a different username (but same email)
    u1.setUserName("user_X");
    result = service.create(u1);
    assertEquals("Expected USER_ALREADY_EXIST (user already exists)", ModelError.USER_ALREADY_EXIST, result.getError());

    u1.setUserName("");
    u1.setEmail("email@email.com");
    result = service.create(u1);
    assertEquals("Expected CONSTRAINT_VIOLATION for empty username", ModelError.CONSTRAINT_VIOLATION, result.getError());
  }

  /**
   * Checks the typical session creation processes.
   */
  @Test
  public void testSessions() throws Exception {
    UserCreation u1 = generateUser();
    u1.setUserName("frank");
    u1.setFirstName("Tim");
    u1.setLastName("Robertson");
    u1.setEmail("frank@gbif.org");
    service.create(u1);

    // this will create a session
    /*
    Session s = service.authenticate(u1.getUserName(), "password");
    User u2 = service.getBySession(s.getSession());
    assertEquals(u1.getUserName(), u2.getUserName());
    assertEquals(1, service.listSessions(u1.getUserName()).size());

    s = service.authenticate(u1.getUserName(), "password", "127.0.0.1");
    User u3 = service.getBySession(s.getSession());
    assertEquals(u1.getUserName(), u3.getUserName());
    assertEquals(2, service.listSessions(u1.getUserName()).size());

    s = service.authenticate(u1.getUserName(), "password", "127.0.0.1");
    User u4 = service.getBySession(s.getSession());
    assertEquals(u1.getUserName(), u4.getUserName());
    assertEquals(3, service.listSessions(u1.getUserName()).size());

    service.terminateSession(s.getSession());
    assertEquals(2, service.listSessions(u1.getUserName()).size());
    assertNull(service.getBySession(s.getSession()));

    service.terminateAllSessions(u1.getUserName());
    assertTrue(service.listSessions(u1.getUserName()).isEmpty());
     */
  }

  @Test
  public void testCreateUserChallengeCodeSequence() {
    User user = createConfirmedUser(service, emailManager);
    assertNotNull(user);
  }

  @Test
  public void testResetPasswordSequence() {
    User user = createConfirmedUser(service, emailManager);
    service.resetPassword(user.getKey());

    //ensure we can not login
    assertNull("Can not login until the password is changed", service.authenticate(user.getUserName(), TEST_PASSWORD));

    //confirm challenge code
    UUID challengeCode = emailManager.getChallengeCode(user.getEmail());
    assertNotNull("Got a challenge code", challengeCode);
    assertTrue("password can be changed using challengeCode", service.updatePassword(user.getKey(), TEST_PASSWORD2, challengeCode));

    //ensure we can now login
    assertNotNull("Can login after the challenge code is confirmed", service.authenticate(user.getUserName(), TEST_PASSWORD2));
  }

  /**
   * Generates a different user on each call.
   * Thread-Safe
   * @return
   */
  private static UserCreation generateUser() {
    int idx = index.incrementAndGet();
    UserCreation user = new UserCreation();
    user.setUserName("user_" + idx);
    user.setFirstName("Tim");
    user.setLastName("Robertson");
    user.setPassword(TEST_PASSWORD);
    user.getRoles().add(UserRole.USER);
    user.getSettings().put("user.settings.language", "en");
    user.getSettings().put("user.country", "dk");
    user.setEmail("user_" + idx + "@gbif.org");
    return user;
  }

  /**
   * Creates a new user and confirms its challenge code.
   * No assertion performed.
   * @param service
   * @param emailManager
   * @return
   */
  private static User createConfirmedUser(IdentityService service, IdentityEmailManagerMock emailManager) {
    UserCreation u1 = generateUser();
    // create the user
    UserCreationResult result = service.create(u1);
    assertNotNull("Expected the key to be set", result.getKey());
    u1.setKey(result.getKey());

    //ensure we can not login
    assertNull("Can not login until the challenge code is confirmed", service.authenticate(u1.getUserName(), TEST_PASSWORD));

    //confirm challenge code
    UUID challengeCode = emailManager.getChallengeCode(u1.getEmail());
    assertNotNull("Got a challenge code", challengeCode);
    assertTrue("challengeCode can be confirmed", service.confirmChallengeCode(u1.getKey(), challengeCode));

    //ensure we can now login
    assertNotNull("Can login after the challenge code is confirmed", service.authenticate(u1.getUserName(), TEST_PASSWORD));

    return service.getByKey(u1.getKey());
  }
}
