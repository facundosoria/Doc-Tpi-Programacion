package ar.edu.utn.frc.tup.piv.evaluacionllm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mensajes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MensajeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversacion_id", nullable = false)
    private ConversacionEntity conversacion;

    @Column(name = "rol", nullable = false, length = 30)
    private String rol; // "alumno", "tutor", "sistema"

    @Column(name = "contenido", nullable = false, length = 10000)
    private String contenido;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
