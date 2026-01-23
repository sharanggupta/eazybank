package dev.sharanggupta.account.mapper;

import dev.sharanggupta.account.dto.AccountDto;
import dev.sharanggupta.account.entity.Account;

public class AccountMapper {

    public static AccountDto mapToAccountDto(Account source, AccountDto destination) {
        destination.setAccountNumber(source.getAccountNumber());
        destination.setAccountType(source.getAccountType());
        destination.setBranchAddress(source.getBranchAddress());
        return destination;
    }

    public static Account mapToAccount(AccountDto source, Account destination) {
        destination.setAccountType(source.getAccountType());
        destination.setBranchAddress(source.getBranchAddress());
        return destination;
    }
}