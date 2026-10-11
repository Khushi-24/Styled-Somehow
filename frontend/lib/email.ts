export const GMAIL_PATTERN="[a-zA-Z0-9][a-zA-Z0-9._+\\-]*@[gG][mM][aA][iI][lL]\\.[cC][oO][mM]";
export const GMAIL_HINT="Only @gmail.com addresses are accepted.";
export function isGmail(email:string){return /^[a-z0-9][a-z0-9._+\-]*@gmail\.com$/i.test(email.trim());}
