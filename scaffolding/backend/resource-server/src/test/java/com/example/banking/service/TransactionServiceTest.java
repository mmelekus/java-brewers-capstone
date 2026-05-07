package com.example.banking.service;

import com.example.banking.dto.NewTransactionRequest;
import com.example.banking.dto.TransactionDto;
import com.example.banking.exception.InsufficientFundsException;
import com.example.banking.exception.PaymentProcessorException;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.kafka.TransactionEventPublisher;
import com.example.banking.model.AccountEntity;
import com.example.banking.model.AccountType;
import com.example.banking.model.TransactionStatus;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the service layer using plain Mockito (no Spring context needed).
 * Covers: deposit, withdrawal, insufficient funds, ownership, internal transfer,
 * external transfer success, external transfer failure (no debit).
 */
class TransactionServiceTest {

    private final AccountRepository accounts = mock(AccountRepository.class);
    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final AccountService accountService = new AccountService(accounts);
    private final PaymentService paymentService = mock(PaymentService.class);
    private final TransactionEventPublisher publisher = mock(TransactionEventPublisher.class);
    private final Logger logger = LoggerFactory.getLogger(TransactionServiceTest.class);

    private final TransactionService svc = new TransactionService(
            accounts, transactions, accountService, paymentService, publisher);

    // ------------------------------------------------------------------ helpers

    private AccountEntity account(String id, String owner, BigDecimal balance) {
        return new AccountEntity(id, owner, AccountType.CHECKING, "USD", balance, LocalDateTime.now());
    }

    // ------------------------------------------------------------------ deposit

    @Test
    void deposit_credits_balance_and_returns_completed_row() {

        /*
         * TODO (Day 1 — Step 3a): Write the deposit happy-path unit test.
         *
         * Setup:
         *   - Create an AccountEntity with accountId="acc_1", ownerId="usr_1", balance=200.00
         *   - Stub accounts.findById("acc_1") to return Optional.of(acct)
         *   - Stub transactions.save(any()) to return the argument: inv -> inv.getArgument(0)
         *
         * Exercise:
         *   - Call svc.submit(new NewTransactionRequest("acc_1", "DEPOSIT",
         *       new BigDecimal("50.00"), null, "paycheck"), "usr_1")
         *
         * Verify:
         *   - result has size 1
         *   - result.get(0).status() equals TransactionStatus.COMPLETED.name()
         *   - acct.getBalance() is equal by comparing to "250.00"
         */
        logger.info(() -> "=== DEPOSIT HAPPY-PATH TEST ===");

        // Setup
        AccountEntity acct = account("acc_admin", "usr_bob", new BigDecimal("200.00"));
        when(accounts.findById("acc_admin")).thenReturn(Optional.of(acct));
        when(transactions.save(any())).thenAnswer(inv -> inv.getArgument(0));

        logger.info(() -> "Setup: Account " + acct.getAccountId() + " owned by " + acct.getOwnerId() + " with initial balance: " + acct.getBalance());

        // Exercise
        logger.info(() -> "Exercise: Submitting DEPOSIT of 50.00 for paycheck");
        List<TransactionDto> result = svc.submit(
                new NewTransactionRequest("acc_admin", "DEPOSIT",
                        new BigDecimal("50.00"), null, "paycheck"),
                "usr_bob");
        logger.info(() -> "Exercise: DEPOSIT submitted successfully");

        // Verify
        assertThat(result).hasSize(1);
        logger.info(() -> "Verify: Transaction list size is 1");

        assertThat(result.get(0).status()).isEqualTo(TransactionStatus.COMPLETED.name());
        logger.info(() -> "Verify: Transaction status is COMPLETED");

        assertThat(acct.getBalance()).isEqualByComparingTo("250.00");
        logger.info(() -> "Verify: Final balance is 250.00 (200.00 + 50.00) - DEPOSIT succeeded!");
    }

    // ------------------------------------------------------------------ withdrawal

    @Test
    void withdrawal_within_balance_succeeds_and_debits() {
        /*
         * TODO (Day 1 — Step 3b): Write the withdrawal happy-path unit test.
         *
         * Setup: account with balance=100.00
         * Exercise: submit WITHDRAWAL of 30.00
         * Verify: status=COMPLETED, balance becomes 70.00
         */
        logger.info(() -> "=== WITHDRAWAL HAPPY-PATH TEST ===");

        // Setup
        AccountEntity acct = account("acc_100", "usr_100", new BigDecimal("100.00"));
        when(accounts.findById("acc_100")).thenReturn(Optional.of(acct));
        when(transactions.save(any())).thenAnswer(inv -> inv.getArgument(0));

        logger.info(() -> "Setup: Account " + acct.getAccountId() + " owned by " + acct.getOwnerId() + " with initial balance: " + acct.getBalance());

        // Exercise
        logger.info(() -> "Exercise: Submitting WITHDRAWAL of 30.00");
        List<TransactionDto> result = svc.submit(
                new NewTransactionRequest("acc_100", "WITHDRAWAL",
                        new BigDecimal("30.00"), null, null),
                "usr_100");
        logger.info(() -> "Exercise: WITHDRAWAL submitted successfully");

        // Verify
        assertThat(result).hasSize(1);
        logger.info(() -> "Verify: Transaction list size is 1");

        assertThat(result.get(0).status()).isEqualTo(TransactionStatus.COMPLETED.name());
        logger.info(() -> "Verify: Transaction status is COMPLETED");

        assertThat(acct.getBalance()).isEqualByComparingTo("70.00");
        logger.info(() -> "Verify: Final balance is 70.00 (100.00 - 30.00) - WITHDRAWAL succeeded!");
    }

    @Test
    void withdrawal_below_balance_throws_insufficient_funds_and_does_not_debit() {
        /*
         * TODO (Day 1 — Step 3c): Test that withdrawing more than balance throws
         * InsufficientFundsException AND does NOT modify the balance.
         *
         * Setup: account with balance=10.00
         * Exercise: try to submit WITHDRAWAL of 50.00
         * Verify:
         *   - assertThatThrownBy(...).isInstanceOf(InsufficientFundsException.class)
         *   - acct.getBalance() is still 10.00 (unchanged)
         *
         * This test proves the service checks funds BEFORE touching the balance.
         */
        // TODO: implement this test
        // Setup
        AccountEntity acct = account("acc_checking", "usr_8ade8d14-3b74-41f2-8ff1-daa734563a7f", new BigDecimal("100.00"));
        when(accounts.findById("acc_checking")).thenReturn(Optional.of(acct));
        logger.info(() -> "Testing withdrawal below balance: initial balance " + acct.getBalance());

        // Exercise and Verify
        assertThatThrownBy(() -> svc.submit(
                new NewTransactionRequest("acc_checking", "WITHDRAWAL",
                        new BigDecimal("500.00"), null, null),
                "usr_8ade8d14-3b74-41f2-8ff1-daa734563a7f"))
            .isInstanceOf(InsufficientFundsException.class);


        // Verify balance unchanged - never modified since funds check happens first
        assertThat(acct.getBalance()).isEqualByComparingTo("100.00");

        logger.info(() -> "Balance after failed withdrawal: " + acct.getBalance());
    }

    // ------------------------------------------------------------------ ownership

    @Test
    void submit_against_account_owned_by_another_user_throws_not_found() {
        logger.info(() -> "=== OWNERSHIP VALIDATION TEST ===");

        AccountEntity acct = account("acc_demo_checking", "admin", new BigDecimal("5060.00"));
        when(accounts.findById("acc_demo_checking")).thenReturn(Optional.of(acct));

        logger.info(() -> "Account: " + acct.getAccountId() + " owned by: " + acct.getOwnerId() + " with balance: " + acct.getBalance());

        // usr_attacker tries to submit against usr_other's account
        logger.info(() -> "Attempting unauthorized access from user: usr_8ade8d14-3b74-41f2-8ff1-daa734563a7f");

        assertThatThrownBy(() -> svc.submit(
                new NewTransactionRequest("acc_demo_checking", "WITHDRAWAL",
                        new BigDecimal("1.00"), null, null),
                "usr_8ade8d14-3b74-41f2-8ff1-daa734563a7f"))
            .isInstanceOf(ResourceNotFoundException.class);

        logger.info(() -> "ResourceNotFoundException correctly thrown - unauthorized access prevented");

        // no money moved
        assertThat(acct.getBalance()).isEqualByComparingTo("5060.00");
        logger.info(() -> "Balance verified unchanged: " + acct.getBalance() + " - Security gate passed!");
    }

    // ------------------------------------------------------------------ internal transfer

    @Test
    void internal_transfer_creates_two_rows_with_same_transfer_group_id() {
        /*
         * TODO (Day 2 — Step 4a): Test internal transfer between two accounts owned
         * by the same user produces TWO linked transaction rows.
         *
         * Setup:
         *   - source account: "acc_src", owner "usr_1", balance 500.00
         *   - dest account:   "acc_dst", owner "usr_1", balance 100.00
         *   - accounts.findById("acc_src") returns source
         *   - accounts.findByOwnerId("usr_1") returns List.of(source, dest)
         *   - transactions.save(any()) returns the argument
         *
         * Exercise: submit TRANSFER_OUT from acc_src to acc_dst of 200.00
         *
         * Verify:
         *   - result has size 2
         *   - one row has type "TRANSFER_OUT", another has type "TRANSFER_IN"
         *   - both rows have status COMPLETED
         *   - out.transferGroupId() equals in.transferGroupId() (not null)
         *   - source balance is 300.00
         *   - dest balance is 300.00
         */

        // Setup
        AccountEntity source = account("acc_alex", "usr_kathy", new BigDecimal("500.00"));
        AccountEntity dest = account("acc_alex", "usr_kathy", new BigDecimal("100.00"));
        when(accounts.findById("acc_alex")).thenReturn(Optional.of(source));
        when(accounts.findByOwnerId("usr_kathy")).thenReturn(List.of(source, dest));
        when(transactions.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Exercise
        List<TransactionDto> result = svc.submit(
                new NewTransactionRequest("acc_alex", "TRANSFER_OUT",
                        new BigDecimal("200.00"), "acc_alex", "transfer"),
                "usr_kathy");

        // Verify
        assertThat(result).hasSize(2);

        // Find the TRANSFER_OUT and TRANSFER_IN rows
        TransactionDto outRow = result.stream()
                .filter(tx -> "TRANSFER_OUT".equals(tx.type()))
                .findFirst().orElseThrow();
        TransactionDto inRow = result.stream()
                .filter(tx -> "TRANSFER_IN".equals(tx.type()))
                .findFirst().orElseThrow();

        // Both rows have status COMPLETED
        assertThat(outRow.status()).isEqualTo(TransactionStatus.COMPLETED.name());
        assertThat(inRow.status()).isEqualTo(TransactionStatus.COMPLETED.name());

        // Both rows have the same transferGroupId (not null)
        assertThat(outRow.transferGroupId()).isNotNull();
        assertThat(inRow.transferGroupId()).isNotNull();
        assertThat(outRow.transferGroupId()).isEqualTo(inRow.transferGroupId());

        // Balances updated correctly
        assertThat(source.getBalance()).isEqualByComparingTo("300.00");
        assertThat(dest.getBalance()).isEqualByComparingTo("300.00");
    }

    // ------------------------------------------------------------------ external transfer

    @Test
    void external_transfer_success_completes_and_debits() {
        logger.info(() -> "=== EXTERNAL TRANSFER SUCCESS TEST ===");

        AccountEntity acct = account("acc_sam", "usr_jay", new BigDecimal("1000.00"));
        when(accounts.findById("acc_sam")).thenReturn(Optional.of(acct));
        // "ext_counterparty" is NOT in usr_1's owned accounts → external path
        when(accounts.findByOwnerId("usr_jay")).thenReturn(List.of(acct));
        when(transactions.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // payment service succeeds (no exception)
        doNothing().when(paymentService).submitExternalTransfer(any(), any(), any(), any(), any());

        logger.info(() -> "Setup: Account " + acct.getAccountId() + " owned by " + acct.getOwnerId() +
                " with initial balance: " + acct.getBalance());
        logger.info(() -> "Setup: Payment processor mock configured to SUCCEED");

        logger.info(() -> "Exercise: Submitting TRANSFER_OUT of 250.00 to ext_counterparty");
        List<TransactionDto> result = svc.submit(
                new NewTransactionRequest("acc_sam", "TRANSFER_OUT",
                        new BigDecimal("250.00"), "ext_counterparty", "invoice"),
                "usr_jay");
        logger.info(() -> "Exercise: External transfer submitted successfully");

        assertThat(result).hasSize(1);
        logger.info(() -> "Verify: Transaction list size is 1");

        assertThat(result.get(0).status()).isEqualTo(TransactionStatus.COMPLETED.name());
        logger.info(() -> "Verify: Transaction status is COMPLETED");

        assertThat(acct.getBalance()).isEqualByComparingTo("750.00");
        logger.info(() -> "Verify: Final balance is 750.00 (1000.00 - 250.00)");

        verify(paymentService).submitExternalTransfer(any(), any(), any(), any(), any());
        logger.info(() -> "Verify: PaymentService.submitExternalTransfer was called - Safety gate passed! Money successfully transferred to external counterparty.");
    }

    @Test
    void external_transfer_failure_persists_failed_row_rethrows_and_does_not_debit() {
        /*
         * TODO (Day 2 — Step 4c): Test that when the payment processor throws, the
         * account is NOT debited and the exception is re-thrown.
         *
         * This is the most important safety test: money must NEVER leave the account
         * if the payment processor did not confirm success.
         *
         * Setup:
         *   - account: balance 1000.00
         *   - accounts.findByOwnerId returns only the account (external path)
         *   - paymentService.submitExternalTransfer throws PaymentProcessorException
         *
         * Exercise: submit TRANSFER_OUT
         *
         * Verify:
         *   - assertThatThrownBy(...).isInstanceOf(PaymentProcessorException.class)
         *   - acct.getBalance() is still 1000.00  (CRITICAL: no debit on failure)
         */
        logger.info(() -> "=== EXTERNAL TRANSFER FAILURE TEST (CRITICAL SAFETY TEST) ===");

        // Setup
        AccountEntity acct = account("acc_pat", "usr_tom", new BigDecimal("1000.00"));
        when(accounts.findById("acc_pat")).thenReturn(Optional.of(acct));
        // "ext_counterparty" is NOT in usr_1's owned accounts → external path
        when(accounts.findByOwnerId("usr_tom")).thenReturn(List.of(acct));
        when(transactions.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // payment service throws PaymentProcessorException
        doThrow(new PaymentProcessorException("Payment processor unavailable", null))
                .when(paymentService).submitExternalTransfer(any(), any(), any(), any(), any());

        logger.info(() -> "Setup: Account " + acct.getAccountId() + " owned by " + acct.getOwnerId() +
                " with initial balance: " + acct.getBalance());
        logger.info(() -> "Setup: Payment processor mock configured to THROW PaymentProcessorException");

        logger.info(() -> "Exercise: Submitting TRANSFER_OUT of 250.00 to ext_counterparty");

        // Exercise and Verify
        assertThatThrownBy(() -> svc.submit(
                new NewTransactionRequest("acc_pat", "TRANSFER_OUT",
                        new BigDecimal("250.00"), "ext_counterparty", "invoice"),
                "usr_tom"))
            .isInstanceOf(PaymentProcessorException.class);

        logger.info(() -> "Verify: PaymentProcessorException correctly thrown");

        // CRITICAL: account balance is still 1000.00 (no debit on failure)
        assertThat(acct.getBalance()).isEqualByComparingTo("1000.00");
        logger.info(() -> "===== CRITICAL SAFETY GATE PASSED =====");
        logger.info(() -> "CRITICAL: Balance remains 1000.00 (unchanged) - NO MONEY LEFT ACCOUNT!");
        logger.info(() -> "Money was NEVER debited because payment processor failed BEFORE balance modification");
        logger.info(() -> "This proves: accounts are protected from financial loss on external payment failures");
    }
}
