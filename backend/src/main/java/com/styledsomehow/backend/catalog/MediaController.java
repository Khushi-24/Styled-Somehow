package com.styledsomehow.backend.catalog;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.core.io.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
@RestController
public class MediaController {
 private final Path root;
 public MediaController(@Value("${media.directory}") String directory){root=Path.of(directory).toAbsolutePath().normalize();}
 @PostMapping("/api/admin/media") public Map<String,String> upload(@RequestParam("file") MultipartFile file)throws IOException{
  if(file.isEmpty()||file.getSize()>8*1024*1024)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a photo under 8 MB");
  byte[] bytes=file.getBytes();String type;
  if(bytes.length>12&&bytes[0]==(byte)0xFF&&bytes[1]==(byte)0xD8&&bytes[2]==(byte)0xFF)type="jpg";
  else if(bytes.length>12&&Arrays.equals(Arrays.copyOf(bytes,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))type="png";
  else if(bytes.length>12&&new String(bytes,0,4,java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")&&new String(bytes,8,4,java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP"))type="webp";
  else throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only JPEG, PNG and WebP photos are supported");
  Files.createDirectories(root);String name=UUID.randomUUID()+"."+type;Files.write(root.resolve(name),bytes,StandardOpenOption.CREATE_NEW);return Map.of("url","/api/media/"+name);
 }
 @GetMapping("/api/media/{name}") public ResponseEntity<Resource> photo(@PathVariable String name){
  if(!name.matches("[a-f0-9-]{36}\\.(jpg|png|webp)"))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  Path path=root.resolve(name);if(!Files.isRegularFile(path))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  MediaType type=name.endsWith("png")?MediaType.IMAGE_PNG:name.endsWith("webp")?MediaType.parseMediaType("image/webp"):MediaType.IMAGE_JPEG;
  return ResponseEntity.ok().header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(365)).cachePublic().immutable()).contentType(type).body(new FileSystemResource(path));
 }
}
