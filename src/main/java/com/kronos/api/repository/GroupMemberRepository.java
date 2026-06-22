package com.kronos.api.repository;

import com.kronos.api.model.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    List<GroupMember> findByUserId(Long userId);

    List<GroupMember> findByGroupId(Long groupId);

    Optional<GroupMember> findByUserIdAndGroupId(Long userId, Long groupId);

    Optional<GroupMember> findByUserUuidAndGroupId(UUID userUuid, Long groupId);
}
