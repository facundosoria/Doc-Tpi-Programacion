package com.example.demo.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageDetectionDto {
    private int imageIndex;
    private int pageNumber;
    private int width;
    private int height;
    private String format;
    private String base64Data;
    private String titleHint;
}
