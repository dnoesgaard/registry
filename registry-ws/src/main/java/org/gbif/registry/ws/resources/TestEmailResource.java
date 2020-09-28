/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.ws.resources;

import org.gbif.api.model.directory.Person;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.directory.PersonService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.mail.BaseEmailModel;
import org.gbif.registry.mail.EmailSender;
import org.gbif.registry.mail.organization.OrganizationEmailManager;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/test/email")
@RestController
public class TestEmailResource {

  private final EmailSender emailSender;
  private final PersonService directoryPersonService;
  private final NodeService nodeService;
  private final OrganizationEmailManager emailManager;
  private final OrganizationService organizationService;

  public TestEmailResource(
      EmailSender emailSender,
      PersonService directoryPersonService,
      NodeService nodeService,
      OrganizationEmailManager emailManager,
      OrganizationService organizationService) {
    this.emailSender = emailSender;
    this.directoryPersonService = directoryPersonService;
    this.nodeService = nodeService;
    this.emailManager = emailManager;
    this.organizationService = organizationService;
  }

  @GetMapping
  public void email(
      @RequestParam("orgKey") UUID organizationKey,
      @RequestParam(value = "nodeManagerKey", required = false, defaultValue = "74")
          Integer nodeManagerKey,
      @RequestParam(
              value = "endorsingNodeKey",
              required = false,
              defaultValue = "02c40d2a-1cba-4633-90b7-e36e5e97aba8")
          UUID endorsingNodeKey,
      @RequestParam(value = "type", required = false, defaultValue = "endorsement") String type,
      @RequestParam(value = "email", required = false, defaultValue = "mpodolskiy@gbif.org")
          String email)
      throws Exception {
    Organization organization = organizationService.get(organizationKey);

    if ("endorsement".equals(type)) {
      Person nodeManager = directoryPersonService.get(nodeManagerKey);
      Node endorsingNode = nodeService.get(endorsingNodeKey);

      BaseEmailModel baseEmailModel =
          emailManager.generateOrganizationEndorsementEmailModel(
              organization, nodeManager, UUID.randomUUID(), endorsingNode);
      BaseEmailModel baseEmailModel2 = copyAndSetCustomEmailAddress(baseEmailModel, email);
      emailSender.send(baseEmailModel2);
    } else if ("endorsed".equals(type)) {
      Node endorsingNode = nodeService.get(endorsingNodeKey);

      List<BaseEmailModel> baseEmailModels =
          emailManager.generateOrganizationEndorsedEmailModel(organization, endorsingNode);
      BaseEmailModel baseEmailModel = copyAndSetCustomEmailAddress(baseEmailModels.get(0), email);
      emailSender.send(baseEmailModel);
    } else {
      Contact contact = new Contact();
      contact.setFirstName("Mike");
      BaseEmailModel baseEmailModel =
          emailManager.generateOrganizationPasswordReminderEmailModel(
              organization, contact, email);
      emailSender.send(baseEmailModel);
    }
  }

  private BaseEmailModel copyAndSetCustomEmailAddress(BaseEmailModel another, String email) {
    return new BaseEmailModel(
        Collections.singleton(email),
        another.getSubject(),
        another.getBody(),
        Collections.emptySet());
  }
}
