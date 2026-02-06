package dev.sharanggupta.account.mapper;

import dev.sharanggupta.account.dto.AccountDto;
import dev.sharanggupta.account.entity.Account;

public class AccountMapper {

    private AccountMapper() {
    }

    public static AccountDto mapToDto(Account account) {
        return AccountDto.builder()
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .branchAddress(account.getBranchAddress())
                .build();
    }

    public static Account updateEntity(AccountDto dto, Account account) {
        account.setAccountType(dto.getAccountType());
        account.setBranchAddress(dto.getBranchAddress());
        return account;
    }
}