package com.uniqueleaf.library.policy;

import java.time.LocalDate;

public interface LoanPolicy {
    int maxActiveLoans();

    int loanDurationDays();

    LocalDate calculateDueDate(LocalDate checkoutDate);

    void ensureCanCheckout(int activeLoanCount);
}
