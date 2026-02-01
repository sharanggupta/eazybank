package dev.sharanggupta.account.repository;

import dev.sharanggupta.account.entity.Account;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface AccountRepository extends ReactiveCrudRepository<Account, Long> {
    Mono<Account> findByCustomerId(Long customerId);

    Mono<Void> deleteByCustomerId(Long customerId);
}
