package com.uniqueleaf.library.notification;

import com.uniqueleaf.library.domain.Book;
import com.uniqueleaf.library.domain.Patron;
import com.uniqueleaf.library.event.DomainEventListener;
import com.uniqueleaf.library.event.ReservedBookAvailableEvent;
import com.uniqueleaf.library.exception.BookNotFoundException;
import com.uniqueleaf.library.exception.PatronNotFoundException;
import com.uniqueleaf.library.repository.BookRepository;
import com.uniqueleaf.library.repository.PatronRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReservationNotificationListener implements DomainEventListener<ReservedBookAvailableEvent> {
    private static final Logger logger = LoggerFactory.getLogger(ReservationNotificationListener.class);

    private final NotificationChannel notificationChannel;
    private final PatronRepository patronRepository;
    private final BookRepository bookRepository;

    public ReservationNotificationListener(
            NotificationChannel notificationChannel,
            PatronRepository patronRepository,
            BookRepository bookRepository) {
        this.notificationChannel = notificationChannel;
        this.patronRepository = patronRepository;
        this.bookRepository = bookRepository;
    }

    @Override
    public void onEvent(ReservedBookAvailableEvent event) {
        Patron patron = patronRepository.findById(event.patronId())
                .orElseThrow(() -> new PatronNotFoundException("Patron not found for notification: " + event.patronId()));
        Book book = bookRepository.findByIsbn(event.isbn())
                .orElseThrow(() -> new BookNotFoundException("Book not found for notification: " + event.isbn()));

        NotificationMessage message = new NotificationMessage(
                patron.email(),
                "UniqueLeaf hold ready: " + book.title(),
                "Hello " + patron.name() + ", your reserved LeafCopy of \"" + book.title()
                        + "\" is now on hold at branch " + event.branchId() + ".");
        notificationChannel.send(message);
        logger.info("Notification sent to {} for reservation {}", patron.email(), event.reservationId());
    }
}
