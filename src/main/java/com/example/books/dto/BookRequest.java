package com.example.books.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BookRequest {

    @NotBlank(message = "Title is required!")
    private String title;

    @NotBlank(message = "Author is required!")
    private String author;

    @NotBlank(message = "Category name is required!")
    private String categoryName;
}
