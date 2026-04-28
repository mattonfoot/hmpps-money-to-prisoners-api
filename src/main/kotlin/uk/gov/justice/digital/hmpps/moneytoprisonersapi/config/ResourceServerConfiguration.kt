package uk.gov.justice.digital.hmpps.moneytoprisonersapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandlerImpl
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class ResourceServerConfiguration(
  private val djangoAuthFilter: DjangoOAuth2AuthenticationFilter,
) {

  private val publicPaths = arrayOf(
    "/health/**",
    "/info",
    "/ping",
    "/swagger-ui/**",
    "/v3/api-docs/**",
    "/swagger-ui.html",
    "/prisons/**",
    "/prison_categories/**",
    "/prison_populations/**",
    "/service-availability/**",
    "/notifications/**",
    "/reset_password/**",
    "/change_password/**",
    "/requests/**",
  )

  @Bean("hmppsSecurityFilterChain")
  @Primary
  @Order(1)
  fun securityFilterChain(http: HttpSecurity): SecurityFilterChain = http
    .csrf { it.disable() }
    .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
    .authorizeHttpRequests { auth ->
      auth
        .requestMatchers(*publicPaths).permitAll()
        .anyRequest().authenticated()
    }
    .exceptionHandling {
      it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
      it.accessDeniedHandler(AccessDeniedHandlerImpl())
    }
    .oauth2ResourceServer { it.disable() }
    .addFilterBefore(djangoAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
    .build()
}
