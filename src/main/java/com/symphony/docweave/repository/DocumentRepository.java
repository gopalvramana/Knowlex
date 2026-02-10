package com.symphony.docweave.repository;

import com.symphony.docweave.domain.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    Optional<DocumentEntity> findByChecksum(String checksum);

    Optional<DocumentEntity> findByOriginalFilename(String originalFilename);
}
