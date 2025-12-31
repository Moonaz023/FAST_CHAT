package com.example.fast_chat.config.security;


import com.example.fast_chat.service.security.AppUserDetailsService;
import com.example.fast_chat.service.security.LinkedinOAuth2LoginSuccessHandler;
import com.example.fast_chat.service.security.LinkedinOidUserService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class SecurityConfig {

  JwtAuthFilter jwtAuthFilter;
  private final AppUserDetailsService userDetailsService;
  private static final String[] AUTH_WHITELIST = {
      "/v3/api-docs/**",
      "/swagger-ui/**",
      "/resources/**",
      "/favicon.ico",
  };

  ClientRegistrationRepository clientRegistrationRepository;
  LinkedinOAuth2LoginSuccessHandler linkedinOAuth2LoginSuccessHandler;
  LinkedinOidUserService oidcUserService;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .securityContext(securityContext -> securityContext
            .requireExplicitSave(false)
        )
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) -> {
              if (request.getRequestURI().startsWith("/api/")) {
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter()
                    .write("{\"error\": \"Unauthorized - Invalid or missing JWT\"}");
              } else {
                response.sendRedirect("/oauth2/authorization/google");
                System.out.println("ELSE HIT===============>>>>");
              }
            })
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(AUTH_WHITELIST).permitAll()
            .requestMatchers("/", "/login/**", "/oauth2/**").permitAll()
            .requestMatchers("/api/auth/**", "/api/login").permitAll()
            .requestMatchers("/error", "/ws/**", "/sockjs-node/**").permitAll()
            .requestMatchers("/topic/**").permitAll()
            //.requestMatchers("/api/ai/stream-story").permitAll()
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth2Login -> oauth2Login
            .authorizationEndpoint((authorizationEndpointConfig ->
                authorizationEndpointConfig.authorizationRequestResolver(
                    requestResolver(this.clientRegistrationRepository)
                ))
            )
            .userInfoEndpoint(userInfoEndpoint ->
                userInfoEndpoint
                    .oidcUserService(oidcUserService)
            )
            .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                .accessTokenResponseClient(accessTokenResponseClient()))
            .successHandler(linkedinOAuth2LoginSuccessHandler)
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  private static DefaultOAuth2AuthorizationRequestResolver requestResolver
      (ClientRegistrationRepository clientRegistrationRepository) {
    DefaultOAuth2AuthorizationRequestResolver requestResolver =
        new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository,
            "/oauth2/authorization");
    requestResolver.setAuthorizationRequestCustomizer(c ->
        c.attributes(stringObjectMap -> stringObjectMap.remove(OidcParameterNames.NONCE))
            .parameters(params -> params.remove(OidcParameterNames.NONCE))
    );

    return requestResolver;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:3000","http://192.168.0.105:3000"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Content-Type", "Authorization"));
    config.setExposedHeaders(List.of("Set-Cookie", "Authorization"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    System.out.println("✅ CORS Configured");
    return source;
  }

  private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
    DefaultAuthorizationCodeTokenResponseClient client = new DefaultAuthorizationCodeTokenResponseClient();
    RestTemplate restTemplate = new RestTemplate(Arrays.asList(
        new FormHttpMessageConverter(), new OAuth2AccessTokenResponseHttpMessageConverter()));
    restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
    client.setRestOperations(restTemplate);
    return client;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public DaoAuthenticationProvider daoAuthenticationProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(new BCryptPasswordEncoder());
    return provider;
  }

}
