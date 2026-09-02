package com.example.demo.repository;

import com.example.demo.model.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConversacionRepository extends JpaRepository<Conversacion, UUID> {
}
