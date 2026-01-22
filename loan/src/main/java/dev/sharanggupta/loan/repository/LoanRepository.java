package dev.sharanggupta.loan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.sharanggupta.loan.entity.Loan;

@Repository
public interface LoanRepository extends JpaRepository<Loan, String> {
}
