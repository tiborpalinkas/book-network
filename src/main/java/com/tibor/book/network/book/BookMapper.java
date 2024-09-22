package com.tibor.book.network.book;

import com.tibor.book.network.history.BookTransactionHistory;
import com.tibor.book.network.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface BookMapper {

    @Mapping(target = "archived", constant = "false")
    @Mapping(target = "owner", source = "user")
    Book toBook(BookRequest bookRequest, User user);

    @Mapping(target = "owner", source = "book.owner.fullName")
    BookResponse toBookResponse(Book book);

    @Mapping(target = "id", source = "bookTransactionHistory.book.id")
    @Mapping(target = "title", source = "bookTransactionHistory.book.title")
    @Mapping(target = "authorName", source = "bookTransactionHistory.book.authorName")
    @Mapping(target = "isbn", source = "bookTransactionHistory.book.isbn")
    @Mapping(target = "rate", source = "bookTransactionHistory.book.rate")
    BorrowedBookResponse toBorrowedBookResponse(BookTransactionHistory bookTransactionHistory);
}
