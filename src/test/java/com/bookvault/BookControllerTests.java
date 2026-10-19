package com.bookvault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookvault.entity.Book;
import com.bookvault.repository.BookRepository;

@SpringBootTest
@AutoConfigureMockMvc
class BookControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void cleanDatabase() {
        bookRepository.deleteAll();
    }

    private Book sampleBook() {
        Book book = new Book();
        book.setTitle("Clean Code");
        book.setAuthor("Robert C. Martin");
        book.setIsbn("9780132350884");
        book.setGenre("Software Engineering");
        book.setPrice(new java.math.BigDecimal("499.00"));
        book.setPublishedYear(2008);
        return book;
    }

    private String sampleBookJson() {
        return """
            {
              "title": "Clean Code",
              "author": "Robert C. Martin",
              "isbn": "9780132350884",
              "genre": "Software Engineering",
              "price": 499.00,
              "publishedYear": 2008
            }
            """;
    }

    @Test
    void createBook() throws Exception {
        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sampleBookJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void getAllBooks() throws Exception {
        bookRepository.save(sampleBook());

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Clean Code"));
    }

    @Test
    void getBookById() throws Exception {
        Book saved = bookRepository.save(sampleBook());

        mockMvc.perform(get("/api/books/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author")
                        .value("Robert C. Martin"));
    }

    @Test
    void updateBook() throws Exception {
        Book saved = bookRepository.save(sampleBook());

        String updatedBookJson = """
            {
              "title": "Clean Code - Revised",
              "author": "Robert C. Martin",
              "isbn": "9780132350884",
              "genre": "Software Engineering",
              "price": 499.00,
              "publishedYear": 2008
            }
            """;

        mockMvc.perform(put("/api/books/" + saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedBookJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title")
                        .value("Clean Code - Revised"));
    }

    @Test
    void deleteBook() throws Exception {
        Book saved = bookRepository.save(sampleBook());

        mockMvc.perform(delete("/api/books/" + saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/books/" + saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectBookWithBlankTitle() throws Exception {
        String invalidBookJson = """
            {
              "title": "",
              "author": "Robert C. Martin",
              "isbn": "9780132350884",
              "genre": "Software Engineering",
              "price": 499.00,
              "publishedYear": 2008
            }
            """;

        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBookJson))
                .andExpect(status().isBadRequest());
    }
}
