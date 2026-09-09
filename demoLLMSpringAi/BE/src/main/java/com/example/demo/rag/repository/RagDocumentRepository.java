package com.example.demo.rag.repository;

import com.example.demo.rag.model.RagDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RagDocumentRepository extends JpaRepository<RagDocumentEntity, String> {
    List<RagDocumentEntity> findAllByOrderByUploadedAtDesc();
}
