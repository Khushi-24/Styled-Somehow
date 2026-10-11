package com.styledsomehow.backend.checkout;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public class CheckoutDtos {
 public record Item(@NotBlank @Size(max=100) String slug,@NotBlank @Size(max=50) String colour,@Pattern(regexp="S|M|L|XL") @NotNull String size,@Min(1) @Max(20) int quantity){}
 public record Cart(@NotNull @Size(min=1,max=30) List<@Valid @NotNull Item> items){}
 public record Address(@NotBlank @Size(max=100) String name,@NotBlank @Email @Pattern(regexp=GmailAddress.PATTERN,message="Only @gmail.com addresses are accepted") @Size(max=180) String email,@Pattern(regexp="[6-9][0-9]{9}") @NotNull String phone,@NotBlank @Size(max=200) String line1,@Size(max=200) String line2,@NotBlank @Size(max=100) String city,@NotBlank @Size(max=100) String state,@Pattern(regexp="[1-9][0-9]{5}") @NotNull String pinCode){}
 public record Checkout(@NotNull @Size(min=1,max=30) List<@Valid @NotNull Item> items,@NotNull @Valid Address address,@Pattern(regexp="[a-f0-9-]{36}") @NotNull String requestId,@Min(1) int expectedTotal){}
 public record Line(String slug,String name,String colour,String size,int quantity,int unitPrice,String image,int availableQuantity){}
 public record Quote(List<Line> items,int subtotal,int shipping,int total){}
}
