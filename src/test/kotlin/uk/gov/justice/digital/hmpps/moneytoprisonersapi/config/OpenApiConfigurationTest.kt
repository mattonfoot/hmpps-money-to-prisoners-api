package uk.gov.justice.digital.hmpps.moneytoprisonersapi.config

import io.swagger.v3.oas.models.OpenAPI
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.ContainersConfig

@SpringBootTest
@ActiveProfiles("test")
@Import(ContainersConfig::class)
@DisplayName("OpenAPI Security Configuration")
class OpenApiConfigurationTest {

  @Autowired
  private lateinit var openAPI: OpenAPI

  @Test
  @DisplayName("security scheme is named oauth2_provider")
  fun `security scheme name matches Python API`() {
    val schemes = openAPI.components.securitySchemes
    assertThat(schemes).containsKey("oauth2_provider")
    assertThat(schemes).doesNotContainKey("bearer-jwt")
  }

  @Test
  @DisplayName("security scheme type is oauth2 with password flow")
  fun `security scheme is oauth2 password flow`() {
    val scheme = openAPI.components.securitySchemes["oauth2_provider"]!!
    assertThat(scheme.type.toString()).isEqualTo("oauth2")
    assertThat(scheme.flows).isNotNull
    assertThat(scheme.flows.password).isNotNull
  }

  @Test
  @DisplayName("password flow has token URL and scopes matching Python API")
  fun `password flow configuration matches Python`() {
    val passwordFlow = openAPI.components.securitySchemes["oauth2_provider"]!!.flows.password
    assertThat(passwordFlow.tokenUrl).contains("/oauth2/token/")
    assertThat(passwordFlow.scopes).containsKeys("read", "write")
  }

  @Test
  @DisplayName("global security requirement references oauth2_provider")
  fun `global security references oauth2_provider`() {
    val security = openAPI.security
    assertThat(security).isNotEmpty
    assertThat(security[0]).containsKey("oauth2_provider")
  }
}
