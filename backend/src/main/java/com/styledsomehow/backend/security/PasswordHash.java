package com.styledsomehow.backend.security;
import java.util.Arrays;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class PasswordHash {
 public static void main(String[] args){
  var console=System.console();if(console==null)throw new IllegalStateException("Run in an interactive terminal");
  char[] password=console.readPassword("New admin password (minimum 14 characters): ");
  try{if(password==null||password.length<14)throw new IllegalArgumentException("Use at least 14 characters");System.out.println(new BCryptPasswordEncoder(12).encode(new String(password)));}finally{if(password!=null)Arrays.fill(password,'\0');}
 }
}
