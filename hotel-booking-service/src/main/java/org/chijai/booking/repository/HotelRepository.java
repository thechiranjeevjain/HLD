package org.chijai.booking.repository;

import java.util.List;
import java.util.Optional;
import org.chijai.booking.domain.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<Hotel, Long> {

    Optional<Hotel> findByIdAndDeletedFalse(Long id);

    List<Hotel> findByCityIdAndDeletedFalse(Long cityId);
}
