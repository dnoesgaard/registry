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
package org.gbif.registry.mail.identity;

import org.gbif.api.model.ChallengeCode;
import org.gbif.api.model.common.AbstractGbifUser;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;
import org.gbif.registry.domain.mail.AccountChangeEmailTemplateDataModel;
import org.gbif.registry.domain.mail.AccountDeleteDataModel;
import org.gbif.registry.domain.mail.AccountEmailChangedTemplateDataModel;
import org.gbif.registry.domain.mail.BaseTemplateDataModel;
import org.gbif.registry.domain.mail.ConfirmableTemplateDataModel;
import org.gbif.registry.mail.BaseEmailModel;
import org.gbif.registry.mail.EmailTemplateProcessor;
import org.gbif.registry.mail.EmailType;
import org.gbif.registry.mail.config.IdentitySuretyMailConfigurationProperties;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import freemarker.template.TemplateException;

/** Manager responsible to generate {@link BaseEmailModel}. */
@Service
public class IdentityEmailManager {

  // supported locales
  private static final List<String> SUPPORTED_LOCALES = Arrays.asList("en", "ru");

  private final EmailTemplateProcessor emailTemplateProcessor;
  private final IdentitySuretyMailConfigurationProperties identityMailConfigProperties;
  private final String doiUrl;

  public IdentityEmailManager(
      @Qualifier("identityEmailTemplateProcessor") EmailTemplateProcessor emailTemplateProcessor,
      IdentitySuretyMailConfigurationProperties identityMailConfigProperties,
      @Value("${doi.url}") String doiUrl) {
    this.emailTemplateProcessor = emailTemplateProcessor;
    this.identityMailConfigProperties = identityMailConfigProperties;
    this.doiUrl = doiUrl;
  }

  public BaseEmailModel generateDeleteUserEmailModel(GbifUser user, List<Download> downloads)
      throws IOException {
    try {
      List<String> downloadUrls =
          downloads.stream()
              .filter(
                  download ->
                      download.getStatus() == Download.Status.SUCCEEDED
                          || download.getStatus() == Download.Status.FILE_ERASED)
              .map(download -> doiUrl + download.getDoi())
              .collect(Collectors.toList());

      Locale locale = getLocale(user);

      return emailTemplateProcessor.buildEmail(
          IdentityEmailType.DELETE_ACCOUNT,
          Collections.singleton(user.getEmail()),
          new AccountDeleteDataModel(user.getUserName(), downloadUrls),
          locale);
    } catch (TemplateException e) {
      throw new IOException(e);
    }
  }

  public BaseEmailModel generateNewUserEmailModel(GbifUser user, ChallengeCode challengeCode)
      throws IOException {
    try {
      return generateConfirmationEmailModel(
          user,
          generateConfirmUserUrl(user.getUserName(), challengeCode.getCode()),
          IdentityEmailType.NEW_USER);
    } catch (TemplateException e) {
      throw new IOException(e);
    }
  }

  public BaseEmailModel generateResetPasswordEmailModel(GbifUser user, ChallengeCode challengeCode)
      throws IOException {
    try {
      return generateConfirmationEmailModel(
          user,
          generateResetPasswordUrl(user.getUserName(), challengeCode.getCode()),
          IdentityEmailType.RESET_PASSWORD);
    } catch (TemplateException e) {
      throw new IOException(e);
    }
  }

  public BaseEmailModel generatePasswordChangedEmailModel(GbifUser user) throws IOException {
    try {
      BaseTemplateDataModel dataModel = new BaseTemplateDataModel(user.getUserName());
      Locale locale = getLocale(user);
      return emailTemplateProcessor.buildEmail(
          IdentityEmailType.PASSWORD_CHANGED,
          Collections.singleton(user.getEmail()),
          dataModel,
          locale);
    } catch (TemplateException e) {
      throw new IOException(e);
    }
  }

  public BaseEmailModel generateWelcomeEmailModel(GbifUser user) throws IOException {
    try {
      BaseTemplateDataModel dataModel = new BaseTemplateDataModel(user.getUserName());
      Locale locale = getLocale(user);
      return emailTemplateProcessor.buildEmail(
          IdentityEmailType.WELCOME, Collections.singleton(user.getEmail()), dataModel, locale);
    } catch (TemplateException e) {
      throw new IOException(e);
    }
  }

  /**
   * Email model that only includes a username and a formatted URL for a specific username and
   * challenge code.
   *
   * @return new {@link BaseEmailModel} or null if an error occurred
   */
  private BaseEmailModel generateConfirmationEmailModel(GbifUser user, URL url, EmailType emailType)
      throws IOException, TemplateException {
    BaseTemplateDataModel dataModel = new ConfirmableTemplateDataModel(user.getUserName(), url);
    Locale locale = getLocale(user);
    return emailTemplateProcessor.buildEmail(
        emailType, Collections.singleton(user.getEmail()), dataModel, locale);
  }

  public BaseEmailModel generateAccountEmailChangeEmailModel(
      GbifUser user, String newEmail, ChallengeCode challengeCode) throws IOException {
    try {
      URL url = generateChangeEmailUrl(user.getUserName(), challengeCode.getCode(), newEmail);
      BaseTemplateDataModel dataModel =
          new AccountChangeEmailTemplateDataModel(
              user.getUserName(), url, user.getEmail(), newEmail);
      Locale locale = getLocale(user);
      return emailTemplateProcessor.buildEmail(
          IdentityEmailType.CHANGE_EMAIL, Collections.singleton(newEmail), dataModel, locale);
    } catch (TemplateException e) {
      throw new IOException(e);
    }
  }

  public BaseEmailModel generateAccountEmailChangedEmailModel(GbifUser user, String oldEmail)
      throws IOException {
    try {
      BaseTemplateDataModel dataModel =
          new AccountEmailChangedTemplateDataModel(user.getUserName(), user.getEmail());
      Locale locale = getLocale(user);
      return emailTemplateProcessor.buildEmail(
          IdentityEmailType.EMAIL_CHANGED,
          Sets.newHashSet(user.getEmail(), oldEmail),
          dataModel,
          locale);
    } catch (TemplateException e) {
      throw new IOException(e);
    }
  }

  private URL generateConfirmUserUrl(String userName, UUID confirmationKey)
      throws MalformedURLException {
    return new URL(
        MessageFormat.format(
            identityMailConfigProperties.getUrlTemplate().getConfirmUser(),
            userName,
            confirmationKey.toString()));
  }

  private URL generateResetPasswordUrl(String userName, UUID confirmationKey)
      throws MalformedURLException {
    return new URL(
        MessageFormat.format(
            identityMailConfigProperties.getUrlTemplate().getResetPassword(),
            userName,
            confirmationKey.toString()));
  }

  private URL generateChangeEmailUrl(String userName, UUID confirmationKey, String email)
      throws MalformedURLException, UnsupportedEncodingException {
    return new URL(
        MessageFormat.format(
            identityMailConfigProperties.getUrlTemplate().getChangeEmail(),
            userName,
            confirmationKey.toString(),
            URLEncoder.encode(email, StandardCharsets.UTF_8.name())));
  }

  private Locale getLocale(GbifUser user) {
    return Optional.ofNullable(user)
        .map(AbstractGbifUser::getLocale)
        .map(this::findSuitableLocaleTagAmongAvailable)
        .map(Locale::forLanguageTag)
        .orElse(Locale.ENGLISH);
  }

  private String findSuitableLocaleTagAmongAvailable(Locale locale) {
    return Locale.lookupTag(Locale.LanguageRange.parse(locale.toLanguageTag()), SUPPORTED_LOCALES);
  }
}
