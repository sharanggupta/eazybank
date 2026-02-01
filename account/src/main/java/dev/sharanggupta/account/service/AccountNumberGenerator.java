package dev.sharanggupta.account.service;

import dev.sharanggupta.account.config.AccountNumberGeneratorConfiguration;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Generates structured, unique account numbers for banking operations.
 *
 * Account number format: BBBBCCNNNNNNNNNNCD (17 digits total)
 * - BBBB: Bank code (4 digits)
 * - CC: Branch code (2 digits)
 * - NNNNNNNNNN: Sequential number (10 digits)
 * - D: Luhn check digit (1 digit)
 */
@Component
public class AccountNumberGenerator {

    private static final Random random = new Random();

    /**
     * Generates a structured account number with Luhn check digit.
     *
     * @return A 17-digit account number string
     */
    public String generate() {
        String sequentialNumber = generateSequentialNumber();
        String accountWithoutCheckDigit = AccountNumberGeneratorConfiguration.BANK_CODE
                + AccountNumberGeneratorConfiguration.BRANCH_CODE.substring(0, 2)
                + sequentialNumber;
        int checkDigit = calculateLuhnCheckDigit(accountWithoutCheckDigit);
        return accountWithoutCheckDigit + checkDigit;
    }

    /**
     * Generates a random sequential number for uniqueness.
     *
     * @return A 10-digit sequential number string
     */
    private String generateSequentialNumber() {
        return String.format("%010d", random.nextLong(10000000000L));
    }

    /**
     * Calculates the Luhn check digit for account number validation.
     * The Luhn algorithm is used in banking to validate account numbers and detect errors.
     *
     * @param accountNumber The account number without check digit
     * @return The calculated Luhn check digit (0-9)
     */
    private int calculateLuhnCheckDigit(String accountNumber) {
        int sum = 0;
        boolean alternate = false;

        for (int i = accountNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(accountNumber.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return (10 - (sum % 10)) % 10;
    }
}
