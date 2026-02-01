package dev.sharanggupta.account.config;

public final class AccountNumberGeneratorConfiguration {

    // Bank code: 4 digits (identifies the bank)
    public static final String BANK_CODE = "0001";  // ICIC Bank

    // Branch code: 2 digits (identifies the branch)
    public static final String BRANCH_CODE = "0050";  // New York branch

    // Account number format: BBBBCCNNNNNNNNNNCD
    // - BBBB: Bank code (4 digits)
    // - CC: Branch code (2 digits)
    // - NNNNNNNNNN: Sequential number (10 digits)
    // - D: Luhn check digit (1 digit)
    public static final String ACCOUNT_NUMBER_FORMAT = "Bank Code (4) + Branch Code (2) + Sequential (10) + Check Digit (1)";
    public static final int ACCOUNT_NUMBER_LENGTH = 17;
    public static final int SEQUENTIAL_NUMBER_LENGTH = 10;

    private AccountNumberGeneratorConfiguration() {
        throw new AssertionError("Cannot instantiate utility class");
    }
}
