package com.styledsomehow.backend.catalog;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name="products")
public class Product {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Version public Long version;
 @NotBlank @Size(max=100) @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") @Column(unique=true,nullable=false,length=100) public String slug;
 @NotBlank @Size(max=180) @Column(nullable=false) public String name;
 @Min(1) @Max(1000000) public int price;
 @Min(1) @Max(1000000) public int originalPrice;
 @NotBlank @Size(max=2000) @Column(length=2000) public String description;
 @NotBlank @Size(max=4000) @Column(length=4000) public String details;
 @NotBlank @Size(max=100) public String composition;
 @Min(1) @Max(1000) public int gsm;
 public boolean newIn;
 @Min(0) public int sortOrder;
 @Min(0) public int collectionOrder;
 @NotNull @Enumerated(EnumType.STRING) @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR) public Status status;
 @ElementCollection @CollectionTable(name="product_collections",joinColumns=@JoinColumn(name="product_id")) @Column(name="value") @OrderColumn(name="position") @Size(min=1,max=2) public List<@Pattern(regexp="men|women") String> collections=new ArrayList<>();
 @ElementCollection @CollectionTable(name="product_colours",joinColumns=@JoinColumn(name="product_id")) @Column(name="value") @OrderColumn(name="position") @Size(min=1,max=10) public List<@NotBlank @Size(max=50) String> colours=new ArrayList<>();
 @ElementCollection @CollectionTable(name="product_sizes",joinColumns=@JoinColumn(name="product_id")) @Column(name="value") @OrderColumn(name="position") @Size(min=1,max=4) public List<@Pattern(regexp="S|M|L|XL") String> sizes=new ArrayList<>();
 @ElementCollection @CollectionTable(name="product_media",joinColumns=@JoinColumn(name="product_id")) @OrderColumn(name="position") @Valid @Size(min=1,max=20) public List<Media> media=new ArrayList<>();
 public enum Status { DRAFT, PUBLISHED, ARCHIVED }
 @Embeddable public static class Media {
  @NotBlank @Size(max=255) @Pattern(regexp="/(?:images|videos|api/media)/[a-zA-Z0-9_-]+\\.(?:webp|jpg|jpeg|png|mp4)") public String url;
  @NotBlank @Size(max=240) public String alt;
  @NotBlank @Size(max=50) public String colour;
  @Pattern(regexp="image|video") @NotNull public String kind;
  @Column(name="contain_image") public boolean contain;
 }
 @JsonIgnore @AssertTrue(message="Original price must be at least the selling price") public boolean isPriceValid(){return originalPrice>=price;}
 @JsonIgnore @AssertTrue(message="Each colour needs a photo; media colours and types must match") public boolean isMediaValid(){
  if(media==null||colours==null) return false;
  return !media.isEmpty() && media.getFirst()!=null && "image".equals(media.getFirst().kind) && media.stream().allMatch(m->m!=null && colours.contains(m.colour) && m.url!=null && ("video".equals(m.kind)==m.url.endsWith(".mp4"))) && colours.stream().allMatch(c->media.stream().anyMatch(m->m!=null&&c.equals(m.colour)&&"image".equals(m.kind)));
 }
 @JsonIgnore @AssertTrue(message="Collections, colours and sizes cannot contain duplicates") public boolean isListsValid(){return collections!=null&&colours!=null&&sizes!=null&&collections.stream().distinct().count()==collections.size()&&colours.stream().distinct().count()==colours.size()&&sizes.stream().distinct().count()==sizes.size();}
}
