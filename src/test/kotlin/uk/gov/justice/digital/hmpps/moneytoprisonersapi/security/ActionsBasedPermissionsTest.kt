package uk.gov.justice.digital.hmpps.moneytoprisonersapi.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod

@DisplayName("ActionsBasedPermissions (COR-020)")
class ActionsBasedPermissionsTest {

  @Test
  fun `COR-020 POST maps to add permission`() {
    assertThat(ActionsBasedPermissions.permissionFor(HttpMethod.POST)).isEqualTo("add")
  }

  @Test
  fun `COR-020 PUT maps to change permission`() {
    assertThat(ActionsBasedPermissions.permissionFor(HttpMethod.PUT)).isEqualTo("change")
  }

  @Test
  fun `COR-020 PATCH maps to change permission`() {
    assertThat(ActionsBasedPermissions.permissionFor(HttpMethod.PATCH)).isEqualTo("change")
  }

  @Test
  fun `COR-020 DELETE maps to delete permission`() {
    assertThat(ActionsBasedPermissions.permissionFor(HttpMethod.DELETE)).isEqualTo("delete")
  }

  @Test
  fun `COR-020 GET returns null (no explicit permission required beyond authentication)`() {
    assertThat(ActionsBasedPermissions.permissionFor(HttpMethod.GET)).isNull()
  }

  @Test
  fun `COR-020 HEAD returns null`() {
    assertThat(ActionsBasedPermissions.permissionFor(HttpMethod.HEAD)).isNull()
  }
}
