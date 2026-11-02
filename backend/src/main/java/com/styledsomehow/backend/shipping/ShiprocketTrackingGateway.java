package com.styledsomehow.backend.shipping;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import java.net.URI;
import java.util.*;
@Component
public class ShiprocketTrackingGateway implements TrackingGateway {
 private final ShiprocketClient api;
 public ShiprocketTrackingGateway(ShiprocketClient api){this.api=api;}
 public Snapshot track(String awb){
  if(awb==null||!awb.matches("[A-Za-z0-9-]{1,100}"))throw new IllegalArgumentException("Invalid tracking number");
  return parse(api.get("/courier/track/awb/"+awb,Map.of()),awb);
 }
 static Snapshot parse(JsonNode response,String awb){
  var data=response.path("tracking_data");var track=data.path("shipment_track").path(0);
  if(data.path("track_status").asInt(0)!=1||!awb.equals(track.path("awb_code").asText()))throw new IllegalStateException("Tracking is not available for this AWB yet");
  String label=track.path("current_status").asText("").trim();if(label.isBlank()||label.length()>150)throw new IllegalStateException("Missing courier status");
  String status=label.toUpperCase(Locale.ROOT).replace('_',' ').replaceAll("\\s+"," ");
  String stage=switch(status){
   case "PICKED UP" -> "SHIPPED";
   case "IN TRANSIT","OUT FOR DELIVERY" -> "IN_TRANSIT";
   case "DELIVERED" -> "DELIVERED";
   case "UNDELIVERED","NDR","RTO INITIATED","RTO IN TRANSIT" -> "DELIVERY_ISSUE";
   case "RTO DELIVERED" -> "RETURNED_TO_ORIGIN";
   default -> null;
  };
  return new Snapshot(label,safeUrl(data.path("track_url").asText("")),stage,List.of("DELIVERED","RTO DELIVERED").contains(status));
 }
 static String safeUrl(String raw){
  if(raw==null||raw.length()>1000)return null;
  try{var uri=URI.create(raw);String host=uri.getHost();if(!"https".equalsIgnoreCase(uri.getScheme())||host==null||uri.getUserInfo()!=null||uri.getPort()!=-1)return null;
   host=host.toLowerCase(Locale.ROOT);for(String domain:List.of("shiprocket.co","shiprocket.in","delhivery.com","dtdc.in","xpressbees.com","bluedart.com"))if(host.equals(domain)||host.endsWith("."+domain))return raw;
  }catch(IllegalArgumentException ignored){}return null;
 }
}
