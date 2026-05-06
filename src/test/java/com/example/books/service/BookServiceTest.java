package com.example.books.service;

import com.example.books.dto.BookRequest;
import com.example.books.entity.Book;
import com.example.books.entity.Category;
import com.example.books.exception.EntityNotFoundException;
import com.example.books.repository.BookRepository;
import com.example.books.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock private BookRepository bookRepository;
    @Mock private CategoryRepository categoryRepository;

    @InjectMocks
    private BookService bookService;

    private Category category;
    private Book book;
    private BookRequest request;

    @BeforeEach
    void setUp() {
        category = new Category(1L, "Fiction");

        book = Book.builder()
                .id(1L).title("Dune").author("Herbert")
                .category(category).build();

        request = new BookRequest();
        request.setTitle("Dune");
        request.setAuthor("Herbert");
        request.setCategoryName("Fiction");
    }

    // findAll

    @Test
    @DisplayName("findAll: возвращает все книги из репозитория")
    void findAll_returnsBooksFromRepository() {
        when(bookRepository.findAll()).thenReturn(List.of(book));

        List<Book> result = bookService.findAll();

        assertThat(result).hasSize(1).contains(book);
        verify(bookRepository).findAll();
    }

    @Test
    @DisplayName("findAll: пустой репозиторий → пустой список")
    void findAll_emptyRepository_returnsEmptyList() {
        when(bookRepository.findAll()).thenReturn(List.of());

        assertThat(bookService.findAll()).isEmpty();
    }

    // findById

    @Test
    @DisplayName("findById: существующий ID → книга возвращается")
    void findById_exists_returnsBook() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        Book result = bookService.findById(1L);

        assertThat(result.getTitle()).isEqualTo("Dune");
        assertThat(result.getAuthor()).isEqualTo("Herbert");
    }

    @Test
    @DisplayName("findById: несуществующий ID → EntityNotFoundException")
    void findById_notFound_throwsEntityNotFoundException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.findById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // findByTitleAndAuthor

    @Test
    @DisplayName("findByTitleAndAuthor: существующая пара → книга возвращается")
    void findByTitleAndAuthor_exists_returnsBook() {
        when(bookRepository.findByTitleAndAuthor("Dune", "Herbert"))
                .thenReturn(Optional.of(book));

        Book result = bookService.findByTitleAndAuthor("Dune", "Herbert");

        assertThat(result).isEqualTo(book);
    }

    @Test
    @DisplayName("findByTitleAndAuthor: не найдено → EntityNotFoundException")
    void findByTitleAndAuthor_notFound_throwsException() {
        when(bookRepository.findByTitleAndAuthor("Unknown", "Nobody"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.findByTitleAndAuthor("Unknown", "Nobody"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Unknown")
                .hasMessageContaining("Nobody");
    }

    // findAllByCategoryName

    @Test
    @DisplayName("findAllByCategoryName: возвращает книги данной категории")
    void findAllByCategoryName_returnsBooksInCategory() {
        when(bookRepository.findAllByCategoryName("Fiction")).thenReturn(List.of(book));

        List<Book> result = bookService.findAllByCategoryName("Fiction");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory().getName()).isEqualTo("Fiction");
    }

    // create

    @Test
    @DisplayName("create: существующая категория → не создаётся новая, книга сохраняется")
    void create_existingCategory_reusesCategory() {
        when(categoryRepository.findByName("Fiction")).thenReturn(Optional.of(category));
        when(bookRepository.save(any())).thenReturn(book);

        Book result = bookService.create(request);

        assertThat(result.getTitle()).isEqualTo("Dune");
        verify(categoryRepository, never()).save(any()); // новая категория не создаётся
        verify(bookRepository).save(any());
    }

    @Test
    @DisplayName("create: новая категория → создаётся автоматически")
    void create_newCategory_createsAndSaves() {
        Category newCategory = new Category(2L, "SciFi");
        request.setCategoryName("SciFi");

        when(categoryRepository.findByName("SciFi")).thenReturn(Optional.empty());
        when(categoryRepository.save(any())).thenReturn(newCategory);
        when(bookRepository.save(any())).thenAnswer(inv -> {
            Book b = inv.getArgument(0);
            b = Book.builder().id(2L).title(b.getTitle())
                    .author(b.getAuthor()).category(newCategory).build();
            return b;
        });

        Book result = bookService.create(request);

        verify(categoryRepository).save(any()); // категория создана
        assertThat(result.getCategory().getName()).isEqualTo("SciFi");
    }

    @Test
    @DisplayName("create: книга строится с правильными полями")
    void create_bookBuiltWithCorrectFields() {
        when(categoryRepository.findByName("Fiction")).thenReturn(Optional.of(category));
        when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Book result = bookService.create(request);

        assertThat(result.getTitle()).isEqualTo("Dune");
        assertThat(result.getAuthor()).isEqualTo("Herbert");
        assertThat(result.getCategory()).isEqualTo(category);
    }

    // update

    @Test
    @DisplayName("update: книга найдена → обновляет поля и сохраняет")
    void update_exists_updatesBook() {
        BookRequest updateRequest = new BookRequest();
        updateRequest.setTitle("Dune Messiah");
        updateRequest.setAuthor("Herbert");
        updateRequest.setCategoryName("Fiction");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(categoryRepository.findByName("Fiction")).thenReturn(Optional.of(category));
        when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Book result = bookService.update(1L, updateRequest);

        assertThat(result.getTitle()).isEqualTo("Dune Messiah");
        verify(bookRepository).save(any());
    }

    @Test
    @DisplayName("update: книга не найдена → EntityNotFoundException")
    void update_notFound_throwsEntityNotFoundException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.update(99L, request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // deleteById

    @Test
    @DisplayName("deleteById: вызывает deleteById на репозитории")
    void deleteById_callsRepository() {
        bookService.deleteById(1L);
        verify(bookRepository).deleteById(1L);
    }
}