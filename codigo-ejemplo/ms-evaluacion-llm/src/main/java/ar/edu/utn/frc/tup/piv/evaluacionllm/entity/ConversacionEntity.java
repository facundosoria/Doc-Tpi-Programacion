package ar.edu.utn.frc.tup.piv.evaluacionllm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conversaciones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "curso_cohorte_id", nullable = false)
    private String cursoCohorteId;

    @Column(name = "usuario_ref", nullable = false)
    private String usuarioRef;

    @Column(name = "desafio_id")
    private String desafioId;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "estado", nullable = false)
    private String estado;

    @Builder.Default
    @OneToMany(mappedBy = "conversacion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MensajeEntity> mensajes = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (estado == null) {
            estado = "ABIERTA";
        }
    }
}
