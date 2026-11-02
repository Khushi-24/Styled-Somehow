package com.styledsomehow.backend.operations;
import org.springframework.data.jpa.repository.*;
public interface CommerceControlRepository extends JpaRepository<CommerceControl,Long> {
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select c from CommerceControl c where c.id=1") CommerceControl lock();
}
