/*
 * Copyright 2014 Global Biodiversity Information Facility (GBIF)
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
package org.gbif.api.model.common;

import java.util.Date;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.google.common.base.Objects;

/**
 * A GBIF user account registered in the user Identity database (previously Drupal).
 */
public class User extends AbstractUser {

  @NotNull
  private String passwordHash;
  private Date lastLogin;

  @Nullable
  public Date getLastLogin() {
    return lastLogin;
  }

  public void setLastLogin(Date lastLogin) {
    this.lastLogin = lastLogin;
  }

  /**
   * @return the drupal hashed version of the user password.
   */
  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof User)) {
      return false;
    }

    User that = (User) obj;
    return Objects.equal(this.key, that.key)
           && Objects.equal(this.userName, that.userName)
           && Objects.equal(this.firstName, that.firstName)
           && Objects.equal(this.lastName, that.lastName)
           && Objects.equal(this.email, that.email)
           && Objects.equal(this.roles, that.roles)
           && Objects.equal(this.lastLogin, that.lastLogin)
           && Objects.equal(this.passwordHash, that.passwordHash)
           && Objects.equal(this.settings, that.settings);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(key, userName, firstName, lastName, email, roles, lastLogin, passwordHash, settings);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("key", key)
      .add("accountName", userName)
      .add("firstName", firstName)
      .add("lastName", lastName)
      .add("email", email)
      .add("roles", roles)
      .add("lastLogin", lastLogin)
      .add("settings", settings)
      .toString();
  }

}
