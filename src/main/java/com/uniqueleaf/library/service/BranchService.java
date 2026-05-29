package com.uniqueleaf.library.service;

import com.uniqueleaf.library.domain.BookCopy;
import com.uniqueleaf.library.domain.Branch;
import com.uniqueleaf.library.domain.CopyStatus;
import com.uniqueleaf.library.domain.value.BranchId;
import com.uniqueleaf.library.domain.value.CopyId;
import com.uniqueleaf.library.event.BookTransferredEvent;
import com.uniqueleaf.library.event.DomainEventPublisher;
import com.uniqueleaf.library.exception.BranchNotFoundException;
import com.uniqueleaf.library.exception.CopyUnavailableException;
import com.uniqueleaf.library.exception.LibraryException;
import com.uniqueleaf.library.repository.BookCopyRepository;
import com.uniqueleaf.library.repository.BranchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BranchService {
    private static final Logger logger = LoggerFactory.getLogger(BranchService.class);

    private final BranchRepository branchRepository;
    private final BookCopyRepository bookCopyRepository;
    private final DomainEventPublisher eventPublisher;

    public BranchService(
            BranchRepository branchRepository,
            BookCopyRepository bookCopyRepository,
            DomainEventPublisher eventPublisher) {
        this.branchRepository = branchRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.eventPublisher = eventPublisher;
    }

    public Branch addBranch(Branch branch) {
        logger.info("Branch registered: {}", branch.name());
        return branchRepository.save(branch);
    }

    public List<BookCopy> inventoryForBranch(BranchId branchId) {
        if (branchRepository.findById(branchId).isEmpty()) {
            throw new BranchNotFoundException("Branch not found: " + branchId);
        }
        return bookCopyRepository.findByBranchId(branchId);
    }

    public BookCopy transferCopy(CopyId copyId, BranchId targetBranchId) {
        BookCopy copy = bookCopyRepository.findById(copyId)
                .orElseThrow(() -> new CopyUnavailableException("Copy not found: " + copyId));
        if (branchRepository.findById(targetBranchId).isEmpty()) {
            throw new BranchNotFoundException("Branch not found: " + targetBranchId);
        }
        if (copy.status() == CopyStatus.CHECKED_OUT) {
            throw new LibraryException("Checked-out copy cannot be transferred: " + copyId);
        }
        if (copy.status() == CopyStatus.RESERVED_HOLD) {
            throw new LibraryException("Reserved-hold copy cannot be transferred: " + copyId);
        }

        BranchId sourceBranchId = copy.branchId();
        bookCopyRepository.save(copy.withStatus(CopyStatus.IN_TRANSFER));
        BookCopy transferred = copy.moveToBranch(targetBranchId).withStatus(CopyStatus.AVAILABLE);
        bookCopyRepository.save(transferred);
        logger.info("Transfer completed for copy {} from {} to {}", copyId, sourceBranchId, targetBranchId);
        eventPublisher.publish(new BookTransferredEvent(copyId, copy.isbn(), sourceBranchId, targetBranchId));
        return transferred;
    }
}
