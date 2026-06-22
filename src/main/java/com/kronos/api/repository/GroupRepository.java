package com.kronos.api.repository;

import com.kronos.api.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    Optional<Group> findByUuid(UUID uuid);

    Optional<Group> findByInvitationCode(String invitationCode);
}