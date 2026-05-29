package com.uniqueleaf.library.policy;

import com.uniqueleaf.library.exception.LoanLimitExceededException;

import java.time.LocalDate;
import java.util.Objects;

public class StandardLoanPolicy implements LoanPolicy {
    private final int maxActiveLoans;
    private final int loanDurationDays;

    public StandardLoanPolicy() {
        this(5, 21);
    }

    public StandardLoanPolicy(int maxActiveLoans, int loanDurationDays) {
        if (maxActiveLoans <= 0) {
            throw new IllegalArgumentException("maxActiveLoans must be positive");
        }
        if (loanDurationDays <= 0) {
            throw new IllegalArgumentException("loanDurationDays must be positive");
        }
        this.maxActiveLoans = maxActiveLoans;
        this.loanDurationDays = loanDurationDays;
    }

    @Override
    public int maxActiveLoans() {
        return maxActiveLoans;
    }

    @Override
    public int loanDurationDays() {
        return loanDurationDays;
    }

    @Override
    public LocalDate calculateDueDate(LocalDate checkoutDate) {
        return Objects.requireNonNull(checkoutDate, "checkoutDate").plusDays(loanDurationDays);
    }

    @Override
    public void ensureCanCheckout(int activeLoanCount) {
        if (activeLoanCount >= maxActiveLoans) {
            throw new LoanLimitExceededException(
                    "Active loan limit reached. Max allowed is " + maxActiveLoans);
        }
    }
}
