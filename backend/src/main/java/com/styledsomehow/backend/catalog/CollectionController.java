package com.styledsomehow.backend.catalog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;
@RestController
public class CollectionController {
 private final CollectionService service;
 public CollectionController(CollectionService service){this.service=service;}
 public record Create(@NotBlank @Size(max=100) @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") String slug,@NotBlank @Size(max=100) String name){}
 @GetMapping("/api/collections") public List<CollectionService.View> published(){return service.list(false);}
 @GetMapping("/api/admin/collections") public List<CollectionService.View> admin(){return service.list(true);}
 @PostMapping("/api/admin/collections") @ResponseStatus(HttpStatus.CREATED) public CollectionService.View create(@Valid @RequestBody Create input){return service.create(input.slug(),input.name());}
 @DeleteMapping("/api/admin/collections/{slug}") @ResponseStatus(HttpStatus.NO_CONTENT) public void remove(@PathVariable String slug){service.remove(slug);}
}
