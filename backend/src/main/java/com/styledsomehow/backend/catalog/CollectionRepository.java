package com.styledsomehow.backend.catalog;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.List;
public interface CollectionRepository extends JpaRepository<ShopCollection,String> {
 List<ShopCollection> findByActiveTrueOrderBySortOrderAscSlugAsc();
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select c from ShopCollection c order by c.slug")
 List<ShopCollection> lockAll();
}
