package com.example.books;

import com.example.books.config.TestcontainersConfig;
import com.example.books.dto.BookRequest;
import com.example.books.entity.Book;
import com.example.books.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {BookCrudRedisApp.class, TestcontainersConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Testcontainers
@ActiveProfiles("test")
public class BookServiceIntegrationTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void createAndFindBook_shouldUseCacheByTitleAndeAuthor() {
        BookRequest request = new BookRequest();
        request.setTitle("Title");
        request.setAuthor("Author");
        request.setCategoryName("Category");

        Book created = bookService.create(request);

        Book fromDb = bookService.findByTitleAndAuthor("Title", "Author");
        Book fromCache = bookService.findByTitleAndAuthor("Title", "Author");

        assertThat(created.getId()).isNotNull();
        assertThat(fromDb.getId()).isEqualTo(created.getId());
        assertThat(fromCache.getId()).isEqualTo(created.getId());

        Cache cache = cacheManager.getCache("bookByTitleAndAuthor");
        assertThat(cache).isNotNull();

        String key = "Title_Author";
        Book cacheBook = cache.get(key, Book.class);
        assertThat(cacheBook).isNotNull();
        assertThat(cacheBook.getId()).isEqualTo(created.getId());
    }

    @Test
    void findByCategory_shouldCacheListAndInvalidateOnUpdate() {
        BookRequest request = new BookRequest();
        request.setTitle("Title");
        request.setAuthor("Author");
        request.setCategoryName("Category");

        Book created = bookService.create(request);

        List<Book> firstCall = bookService.findAllByCategoryName("Category");
        List<Book> secondCall = bookService.findAllByCategoryName("Category");

        assertThat(firstCall).hasSize(1);
        assertThat(secondCall).hasSize(1);

        Cache cache = cacheManager.getCache("allByCategoryName");
        assertThat(cache).isNotNull();

        @SuppressWarnings("unchecked")
        List<Book> cachedList = cache.get("Category", List.class);
        assertThat(cachedList).isNotNull();
        assertThat(cachedList).hasSize(1);

        BookRequest update = new BookRequest();
        update.setTitle("New Title");
        update.setAuthor("New Author");
        update.setCategoryName("Category");

        bookService.update(created.getId(), update);

        cachedList = cache.get("Category", List.class);
        assertThat(cachedList).isNull();

        List<Book> afterUpdate = bookService.findAllByCategoryName("Category");
        assertThat(afterUpdate).hasSize(1);
        assertThat(afterUpdate.get(0).getTitle()).contains("New");

    }
}
