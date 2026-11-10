package com.toh520.campus.module.library.controller;

import com.toh520.campus.module.auth.model.LoginSession;
import com.toh520.campus.module.library.dto.LibraryRequests;
import com.toh520.campus.module.library.service.LibraryService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LibraryController {
  private final LibraryService library;

  public LibraryController(LibraryService library) {
    this.library = library;
  }

  @GetMapping("/books")
  Object search(@RequestParam(defaultValue = "") String query) {
    return library.search(query);
  }

  @PostMapping("/books/{id}/draft")
  Object draft(@RequestAttribute LoginSession session, @PathVariable long id) {
    return library.draft(session, id);
  }

  @PostMapping("/borrow-records/confirm")
  Object confirm(
      @RequestAttribute LoginSession session, @Valid @RequestBody LibraryRequests.Confirm request) {
    return library.confirm(session, request.token());
  }

  @GetMapping("/borrow-records")
  Object mine(@RequestAttribute LoginSession session) {
    return library.records(session, false);
  }

  @PostMapping("/borrow-records/{id}/return")
  Object returnBook(@RequestAttribute LoginSession session, @PathVariable long id) {
    return library.returnBook(session, id);
  }

  @GetMapping("/admin/borrow-records")
  Object all(@RequestAttribute LoginSession session) {
    return library.records(session, true);
  }

  @PostMapping("/admin/books")
  Object create(
      @RequestAttribute LoginSession session, @Valid @RequestBody LibraryRequests.Book request) {
    library.save(session, null, request);
    return Map.of("message", "图书已新增");
  }

  @PutMapping("/admin/books/{id}")
  Object edit(
      @RequestAttribute LoginSession session,
      @PathVariable long id,
      @Valid @RequestBody LibraryRequests.Book request) {
    library.save(session, id, request);
    return Map.of("message", "图书已更新");
  }

  @DeleteMapping("/admin/books/{id}")
  Object delete(@RequestAttribute LoginSession session, @PathVariable long id) {
    library.delete(session, id);
    return Map.of("message", "图书已删除");
  }
}
