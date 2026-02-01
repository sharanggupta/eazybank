package dev.sharanggupta.loan.repository;

import dev.sharanggupta.loan.entity.Loan;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface LoanRepository extends ReactiveCrudRepository<Loan, Long> {

    Mono<Loan> findByMobileNumber(String mobileNumber);

    Mono<Loan> findByLoanNumber(String loanNumber);
}