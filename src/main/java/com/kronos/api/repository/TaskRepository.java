package com.kronos.api.repository;

import com.kronos.api.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByGroupId(Long groupId);

    List<Task> findByGroupIdOrderByDeadlineAsc(Long groupId);

    Optional<Task> findByUuid(UUID uuid);
}
