package com.kronos.api.model;

import com.kronos.api.model.enums.GroupRole;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_group_member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_role", nullable = false, length = 30)
    private GroupRole groupRole;
}