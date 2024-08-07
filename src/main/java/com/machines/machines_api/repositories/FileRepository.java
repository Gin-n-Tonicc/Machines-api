package com.machines.machines_api.repositories;

import com.machines.machines_api.models.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<File, UUID> {
    Optional<File> findByIdAndDeletedAtIsNull(UUID id);
}

