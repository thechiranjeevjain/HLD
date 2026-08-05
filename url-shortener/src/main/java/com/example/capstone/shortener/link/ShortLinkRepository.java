package com.example.capstone.shortener.link;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortLinkRepository extends JpaRepository<ShortLink, UUID> {

    boolean existsByCode(String code);

    Optional<ShortLink> findByCode(String code);
}
