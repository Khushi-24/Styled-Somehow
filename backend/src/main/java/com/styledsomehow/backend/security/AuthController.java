package com.styledsomehow.backend.security;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
import java.security.Principal;
import java.util.Map;
@RestController
public class AuthController {
 @GetMapping("/api/auth/csrf") public Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken(),"headerName",token.getHeaderName());}
 @GetMapping("/api/auth/me") public Map<String,String> me(Principal principal){return Map.of("username",principal.getName());}
}
