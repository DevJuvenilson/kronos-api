package com.kronos.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Random;
import java.util.UUID;

@Entity
@Table(name = "tb_group")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "invitation_code", nullable = false, unique = true, length = 6)
    private String invitationCode;

    /**
     * Intercepta a criação do grupo para gerar automaticamente o UUID público
     * e o código de convite alfanumérico de 6 dígitos.
     */
    @PrePersist
    protected void onCreate() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }
        if (this.invitationCode == null) {
            this.invitationCode = createInvitationCode();
        }
    }

    /**
     * Método auxiliar para gerar uma string aleatória de 6 caracteres (A-Z, 0-9)
     */
    private String createInvitationCode() {
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(6);

        for (int i = 0; i < 6; i++) {
            int index = random.nextInt(caracteres.length());
            sb.append(caracteres.charAt(index));
        }

        return sb.toString();
    }
}