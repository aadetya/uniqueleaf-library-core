package com.uniqueleaf.library.repository;

import com.uniqueleaf.library.domain.Reservation;
import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.domain.value.CopyId;
import com.uniqueleaf.library.domain.value.Isbn;
import com.uniqueleaf.library.domain.value.PatronId;
import com.uniqueleaf.library.domain.value.ReservationId;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);

    Optional<Reservation> findById(ReservationId reservationId);

    List<Reservation> findByPatronId(PatronId patronId);

    List<Reservation> findByIsbnAndBranchId(Isbn isbn, BranchId branchId);

    List<Reservation> findWaitingByIsbnAndBranchId(Isbn isbn, BranchId branchId);

    Optional<Reservation> findNotifiedByCopyId(CopyId copyId);

    List<Reservation> findAll();
}
