package com.styledsomehow.backend.security;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
@Configuration
public class AdminSecurity {
 @Bean UserDetailsService admin(@Value("${admin.username}") String username,@Value("${admin.password-hash}") String hash){
  if(username.isBlank()||!hash.matches("\\$2[aby]\\$.*"))throw new IllegalStateException("Set ADMIN_USERNAME and ADMIN_PASSWORD_HASH (BCrypt)");
  return new InMemoryUserDetailsManager(User.withUsername(username).password(hash).roles("ADMIN").build());
 }
 @Bean BCryptPasswordEncoder encoder(){return new BCryptPasswordEncoder(12);}
 @Bean SecurityFilterChain security(HttpSecurity http,LoginThrottle throttle)throws Exception{
  http.authorizeHttpRequests(a->a.dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll().requestMatchers("/api/health","/api/products/**","/api/media/**","/api/auth/csrf","/api/auth/login").permitAll().requestMatchers("/api/admin/**","/api/auth/me","/api/auth/logout").hasRole("ADMIN").anyRequest().denyAll())
   .exceptionHandling(e->e.authenticationEntryPoint((r,s,x)->s.sendError(401)).accessDeniedHandler((r,s,x)->s.sendError(403)))
   .formLogin(f->f.loginProcessingUrl("/api/auth/login").successHandler((r,s,a)->s.setStatus(204)).failureHandler((r,s,e)->s.sendError(401)))
   .logout(l->l.logoutUrl("/api/auth/logout").logoutSuccessHandler((r,s,a)->s.setStatus(204)))
   .requestCache(c->c.disable())
   .addFilterBefore(throttle,org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
  return http.build();
 }
}
