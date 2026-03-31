package com.example.books.service;

import com.example.books.dto.BookRequest;
import com.example.books.entity.Book;
import com.example.books.entity.Category;
import com.example.books.exception.EntityNotFoundException;
import com.example.books.repository.BookRepository;
import com.example.books.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

    @Cacheable(value = "allBooks")
    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    @Cacheable(value = "bookById",
                key = "id")
    public Book findById(Long id) {
        return bookRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException(
                        MessageFormat.format("Book with ID {} not found", id)
                )
        );
    }

    @Cacheable(value = "bookByTitleAndAuthor",
                key = "#title + '_' + #author")
    public Book findByTitleAndAuthor(String title, String author) {
        return bookRepository.findByTitleAndAuthor(title, author)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Book not found with title: {0}, author: {1}", title, author)
                ));
    }


    @Cacheable(value = "allByCategoryName",
            key = "#categoryName")
    public List<Book> findAllByCategoryName(String categoryName) {
        return bookRepository.findAllByCategoryName(categoryName);
    }

    @Caching(evict = {
            @CacheEvict(value = "bookByTitleAndAuthor",
                    key = "#request.title + '_' + #request.author",
                    beforeInvocation = true),
            @CacheEvict(value = "allByCategoryName",
                    key = "#request.categoryName",
                    beforeInvocation = true)
    })
    public Book create(BookRequest request) {

        Category category = categoryRepository.findByName(request.getCategoryName())
                .orElseGet(() -> {
                        Category newCategory = new Category();
                        newCategory.setName(request.getCategoryName());
                        return categoryRepository.save(newCategory);
                });

        Book book = Book.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .category(category)
                .build();

        return bookRepository.save(book);
    }

    @Caching(evict = {
            @CacheEvict(value = "bookByTitleAndAuthor",
                    key = "#request.title + '_' + #request.author",
                    beforeInvocation = true),
            @CacheEvict(value = "allByCategoryName",
                    key = "#request.categoryName",
                    beforeInvocation = true)
    })
    public Book update(Long id, BookRequest request) {

        Book existedBook = bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Book with ID {} not found", id)
                ));

        Category category = categoryRepository.findByName(request.getCategoryName())
                .orElseGet(() -> {
                    Category newCategory = new Category();
                    newCategory.setName(request.getCategoryName());
                    return categoryRepository.save(newCategory);
                });

        existedBook.setTitle(request.getTitle());
        existedBook.setAuthor(request.getAuthor());
        existedBook.setCategory(category);

        return bookRepository.save(existedBook);
    }

    @Caching(evict = {
            @CacheEvict(value = "bookByTitleAndAuthor",
                    key = "#result.title + '_' + #result.author"),
            @CacheEvict(value = "allByCategoryName",
                    key = "#result.categoryName")
    })
    public void deleteById(Long id) {
        bookRepository.deleteById(id);
    }
}
