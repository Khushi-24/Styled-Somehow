package com.styledsomehow.backend.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.OncePerRequestFilter;
@Component
public class LoginThrottle extends OncePerRequestFilter {
 private record Window(long start,int count){}
 private final ConcurrentHashMap<String,Window> attempts=new ConcurrentHashMap<>();
 @Bean FilterRegistrationBean<LoginThrottle> registration(){var bean=new FilterRegistrationBean<>(this);bean.setEnabled(false);return bean;}
 protected void doFilterInternal(HttpServletRequest r,HttpServletResponse s,FilterChain chain)throws ServletException,IOException{
  if("POST".equals(r.getMethod())&&"/api/auth/login".equals(r.getServletPath())){
   long now=System.currentTimeMillis();attempts.entrySet().removeIf(e->now-e.getValue().start()>900000);
   // Do not trust an arbitrary X-Forwarded-For header. Proxy deployments need edge rate limiting too.
   Window w=attempts.compute(r.getRemoteAddr(),(k,v)->v==null?new Window(now,1):new Window(v.start(),v.count()+1));
   if(w.count()>10){s.setHeader("Retry-After","900");s.sendError(429);return;}
  }
  chain.doFilter(r,s);
 }
}
