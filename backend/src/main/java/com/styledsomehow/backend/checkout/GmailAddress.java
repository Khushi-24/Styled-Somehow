package com.styledsomehow.backend.checkout;
import java.util.Locale;
public final class GmailAddress {
 private GmailAddress(){}
 public static final String PATTERN="(?i)[a-z0-9][a-z0-9._+\\-]*@gmail\\.com";
 public static boolean valid(String email){return email!=null&&email.matches(PATTERN);}
 // Gmail dots and +tags identify the same inbox; share its request allowance.
 public static String inbox(String email){String local=email.trim().toLowerCase(Locale.ROOT).split("@",2)[0];return local.split("\\+",2)[0].replace(".","")+"@gmail.com";}
}
