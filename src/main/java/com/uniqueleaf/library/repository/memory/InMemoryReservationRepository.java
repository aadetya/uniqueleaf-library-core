package com.uniqueleaf.library.repository.memory;

import com.uniqueleaf.library.domain.Reservation;
import com.uniqueleaf.library.domain.ReservationStatus;
import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.domain.value.CopyId;
import com.uniqueleaf.library.domain.value.Isbn;
import com.uniqueleaf.library.domain.value.PatronId;
import com.uniqueleaf.library.domain.value.ReservationId;
import com.uniqueleaf.library.repository.ReservationRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryReservationRepository implements ReservationRepository {
    private final Map<ReservationId, Reservation> storage = new LinkedHashMap<>();

    @Override
    public Reservation save(Reservation reservation) {
        storage.put(reservation.reservationId(), reservation);
        return reservation;
    }

    @Override
    public Optional<Reservation> findById(ReservationId reservationId) {
        return Optional.ofNullable(storage.get(reservationId));
    }

    @Override
    public List<Reservation> findByPatronId(PatronId patronId) {
        return storage.values().stream()
                .filter(reservation -> reservation.patronId().equals(patronId))
                .sorted(Comparator.comparing(Reservation::createdAt))
                .toList();
    }

    @Override
    public List<Reservation> findByIsbnAndBranchId(Isbn isbn, BranchId branchId) {
        return storage.values().stream()
                .filter(reservation -> reservation.isbn().equals(isbn))
                .filter(reservation -> reservation.branchId().equals(branchId))
                .sorted(Comparator.comparing(Reservation::createdAt))
                .toList();
    }

    @Override
    public List<Reservation> findWaitingByIsbnAndBranchId(Isbn isbn, BranchId branchId) {
        return storage.values().stream()
                .filter(reservation -> reservation.isbn().equals(isbn))
                .filter(reservation -> reservation.branchId().equals(branchId))
                .filter(Reservation::isWaiting)
                .sorted(Comparator.comparing(Reservation::createdAt))
                .toList();
    }

    @Override
    public Optional<Reservation> findNotifiedByCopyId(CopyId copyId) {
        return storage.values().stream()
                .filter(reservation -> reservation.status() == ReservationStatus.NOTIFIED)
                .filter(reservation -> reservation.assignedCopyId().filter(copyId::equals).isPresent())
                .findFirst();
    }

    @Override
    public List<Reservation> findAll() {
        return new ArrayList<>(storage.values());
    }
}
