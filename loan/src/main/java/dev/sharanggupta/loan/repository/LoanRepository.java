package dev.sharanggupta.loan.repository;

import dev.sharanggupta.loan.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, String> {

    Optional<Loan> findByMobileNumber(String mobileNumber);
}