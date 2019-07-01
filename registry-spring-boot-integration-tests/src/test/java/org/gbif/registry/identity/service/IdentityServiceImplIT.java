package org.gbif.registry.identity.service;

import com.google.common.collect.ImmutableMap;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.identity.model.ModelMutationError;
import org.gbif.registry.identity.model.UserModelMutationResult;
import org.gbif.registry.persistence.mapper.ChallengeCodeMapper;
import org.gbif.registry.persistence.mapper.ChallengeCodeSupportMapper;
import org.gbif.registry.surety.email.InMemoryEmailSender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class IdentityServiceImplIT {

  // TODO: 2019-06-28 run liquibase on startup
  // TODO: 2019-06-28 run table truncate each time launch the test?

  private static final String TEST_PASSWORD = "[password]";
  private static final String TEST_PASSWORD2 = "]password[";
  private static final AtomicInteger index = new AtomicInteger(0);

  @Autowired
  private IdentityService identityService;

  @Autowired
  private ChallengeCodeMapper challengeCodeMapper;

  // TODO: 2019-06-28 should be userMapper?
  // TODO: 2019-06-28 rename?
  @Autowired
//  private UserMapper challengeCodeSupportMapper;
  private ChallengeCodeSupportMapper<Integer> challengeCodeSupportMapper;

  @Autowired
  private InMemoryEmailSender inMemoryEmailSender;

  /**
   * Checks the typical CRUD process with correct data only (i.e. no failure scenarios).
   */
  @Test
  public void testCRUD() throws Exception {
    GbifUser u1 = generateUser();

    // create
    UserModelMutationResult result = identityService.create(u1, TEST_PASSWORD);
    assertNotNull("Expected the Username to be set", result.getUsername());

    // get
    GbifUser u2 = identityService.get(u1.getUserName());
    assertEquals(u1.getUserName(), u2.getUserName());
    assertEquals(u1.getFirstName(), u2.getFirstName());
    assertEquals(u1.getLastName(), u2.getLastName());
    assertEquals(2, u2.getSettings().size());
    assertEquals(1, u2.getRoles().size());
    assertNull(u2.getLastLogin());

    // update
    u2.getSettings().put("user.country", "GB");
    u2.getSystemSettings().put("internal.settings", "-7");

    UserModelMutationResult mutationResult = identityService.update(u2);
    assertNotNull("got mutationResult", mutationResult);
    assertFalse("Doesn't contain error like " + mutationResult.getConstraintViolation(), mutationResult.containsError());

    GbifUser u3 = identityService.get(u1.getUserName());
    assertEquals(2, u3.getSettings().size());
    assertEquals("GB", u3.getSettings().get("user.country"));
    assertEquals("-7", u3.getSystemSettings().get("internal.settings"));

    identityService.delete(u1.getKey());
    GbifUser u4 = identityService.get(u1.getUserName());
    assertNull(u4);
  }

  /**
   * Checks the typical CRUD process with correct data only (i.e. no failure scenarios).
   */
  @Test
  public void testCreateError() throws Exception {
    GbifUser u1 = generateUser();
    // create
    UserModelMutationResult result = identityService.create(u1, TEST_PASSWORD);
    assertNotNull("Expected the Username to be set", result.getUsername());

    // try to create it again with a different username (but same email)
    u1.setKey(null); //reset key
    u1.setUserName("user_x");
    result = identityService.create(u1, TEST_PASSWORD);
    assertEquals("Expected USER_ALREADY_EXIST (user already exists)", ModelMutationError.USER_ALREADY_EXIST, result.getError());

    u1.setUserName("");
    u1.setEmail("email@email.com");
    result = identityService.create(u1, TEST_PASSWORD);
    assertEquals("Expected CONSTRAINT_VIOLATION for empty username", ModelMutationError.CONSTRAINT_VIOLATION, result.getError());

    // try with a password too short
    u1.setUserName("user_x");
    result = identityService.create(u1, "p");
    assertEquals("Expected PASSWORD_LENGTH_VIOLATION", ModelMutationError.PASSWORD_LENGTH_VIOLATION, result.getError());
  }

  /**
   * Checks that the get(username) is case insensitive.
   */
  @Test
  public void testGetIsCaseInsensitive() throws Exception {
    GbifUser u1 = generateUser();
    u1.setUserName("testuser");
    u1.setEmail("myEmail@b.com");

    // create
    UserModelMutationResult result = identityService.create(u1, TEST_PASSWORD);
    assertNotNull("Expected the Username to be set. " + result.getConstraintViolation(), result.getUsername());

    GbifUser newUser = identityService.get("tEstuSeR");
    assertNotNull("Can get the user using the same username with capital letters", newUser.getKey());
    //ensure we stored the email by respecting the case
    assertEquals("myEmail@b.com", newUser.getEmail());

    //but we should be able to login using the lowercase version
    newUser = identityService.get("myemail@b.com");
    assertNotNull("Can get the user using the email in lowercase", newUser.getKey());
  }

  @Test
  public void testGetBySystemSetting() throws Exception {
    GbifUser u1 = generateUser();
    u1.setSystemSettings(ImmutableMap.of("my.app.setting", "secret-magic"));

    // create
    UserModelMutationResult result = identityService.create(u1, TEST_PASSWORD);
    assertNotNull("Expected the Username to be set. " + result.getConstraintViolation(), result.getUsername());
    GbifUser newUser = identityService.getBySystemSetting("my.app.setting", "secret-magic");
    assertNotNull("Can get the user using systemSettings", newUser.getKey());

    newUser = identityService.getBySystemSetting("my.app.setting", "wrong-magic");
    assertNull("Can NOT get the user using wrong systemSettings", newUser);
  }

  @Test
  public void testCreateUserChallengeCodeSequence() {
    GbifUser user = createConfirmedUser(identityService, inMemoryEmailSender);
    assertNotNull(user);
  }

  @Test
  public void testResetPasswordSequence() {
    GbifUser user = createConfirmedUser(identityService, inMemoryEmailSender);
    identityService.resetPassword(user.getKey());

    //ensure we can not login
    assertNull("Can not login until the password is changed", identityService.authenticate(user.getUserName(), TEST_PASSWORD));

    //confirm challenge code
    UUID challengeCode = getChallengeCode(user.getKey());
    assertNotNull("Got a challenge code for " + user.getEmail(), challengeCode);
    assertTrue("password can be changed using challengeCode",
        !identityService.updatePassword(user.getKey(), TEST_PASSWORD2, challengeCode).containsError());

    //ensure we can now login
    assertNotNull("Can login after the challenge code is confirmed", identityService.authenticate(user.getUserName(), TEST_PASSWORD2));
  }

  @Test
  public void testCrudEditorRights() {
    GbifUser u1 = generateUser();

    // create
    UserModelMutationResult result = identityService.create(u1, TEST_PASSWORD);
    assertNotNull("Expected the Username to be set", result.getUsername());

    UUID randomUuid = UUID.randomUUID();

    identityService.addEditorRight(result.getUsername(), randomUuid);

    assertEquals(1, identityService.listEditorRights(result.getUsername()).size());
    assertEquals(randomUuid, identityService.listEditorRights(result.getUsername()).get(0));

    identityService.deleteEditorRight(result.getUsername(), randomUuid);

    assertEquals(0, identityService.listEditorRights(result.getUsername()).size());
  }

  /**
   * Generates a different user on each call.
   * Thread-Safe
   * @return
   */
  public static GbifUser generateUser() {
    int idx = index.incrementAndGet();
    GbifUser user = new GbifUser();
    user.setUserName("user_" + idx);
    user.setFirstName("Tim");
    user.setLastName("Robertson");
    user.getRoles().add(UserRole.USER);
    user.getSettings().put("user.settings.language", "en");
    user.getSettings().put("user.country", "dk");
    user.getSystemSettings().put("internal.settings", "18");
    user.setEmail("user_" + idx + "@gbif.org");
    return user;
  }

  /**
   * Creates a new user and confirms its challenge code.
   * No assertion performed.
   * @return
   */
  public GbifUser createConfirmedUser(IdentityService identityService, InMemoryEmailSender inMemoryEmailManager) {
    GbifUser u1 = generateUser();
    // create the user
    UserModelMutationResult result = identityService.create(u1, TEST_PASSWORD);
    assertNotNull("Expected the Username to be set", result.getUsername());

    //ensure we got an email
    assertNotNull("The user got an email with the challenge code", inMemoryEmailManager.getEmail(u1.getEmail()));

    //ensure we can not login
    assertNull("Can not login until the challenge code is confirmed", identityService.authenticate(u1.getUserName(), TEST_PASSWORD));

    UUID challengeCode = getChallengeCode(u1.getKey());
    //confirm challenge code
    assertNotNull("Got a challenge code for email: " + u1.getEmail(), challengeCode);

    GbifUser user = identityService.get(u1.getUserName());
    assertTrue("challengeCode can be confirmed", identityService.confirmUser(u1.getKey(), challengeCode));

    //ensure we can now login
    assertNotNull("Can login after the challenge code is confirmed", identityService.authenticate(u1.getUserName(), TEST_PASSWORD));
    return user;
  }

  private UUID getChallengeCode(Integer entityKey) {
    return challengeCodeMapper.getChallengeCode(challengeCodeSupportMapper.getChallengeCodeKey(entityKey));
  }
}
