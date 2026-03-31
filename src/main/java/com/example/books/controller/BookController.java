package com.example.books.controller;

import com.example.books.dto.BookDTO;
import com.example.books.dto.BookRequest;
import com.example.books.entity.Book;
import com.example.books.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<List<BookDTO>> findAll() {
        List<Book> books = bookService.findAll();
        List<BookDTO> dtos = books.stream()
                .map(BookDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(BookDTO.fromEntity(bookService.findById(id)));
    }

    @GetMapping("/search")
    public ResponseEntity<BookDTO> findByTitleAndAuthor(
            @RequestParam String title, @RequestParam String author) {
        return ResponseEntity.ok(BookDTO.fromEntity(bookService.findByTitleAndAuthor(title, author)));
    }

    @GetMapping("/by-category")
    public ResponseEntity<List<BookDTO>> findByCategory(@RequestParam String categoryName) {
        List<Book> books = bookService.findAllByCategoryName(categoryName);
        List<BookDTO> dtos = books.stream()
                .map(BookDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<BookDTO> create(@RequestBody @Valid BookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BookDTO.fromEntity(bookService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookDTO> update(@PathVariable Long id, @RequestBody @Valid BookRequest request) {
        return ResponseEntity.ok(BookDTO.fromEntity(bookService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
