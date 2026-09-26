package dev.kavrin.banking_ledger.account.application.service;

import dev.kavrin.banking_ledger.account.api.dto.AccountResponse;
import dev.kavrin.banking_ledger.account.application.command.CreateAccountCommand;
import dev.kavrin.banking_ledger.account.domain.model.AccountCategory;
import dev.kavrin.banking_ledger.account.domain.model.AccountStatus;
import dev.kavrin.banking_ledger.account.domain.model.AccountType;
import dev.kavrin.banking_ledger.account.persistence.AccountEntity;
import dev.kavrin.banking_ledger.account.persistence.AccountRepository;
import dev.kavrin.banking_ledger.audit.application.command.WriteAuditEventCommand;
import dev.kavrin.banking_ledger.audit.application.service.AuditEventWriter;
import dev.kavrin.banking_ledger.audit.domain.model.*;
import dev.kavrin.banking_ledger.customer.persistence.CustomerRepository;
import dev.kavrin.banking_ledger.shared.error.ApiErrorCode;
import dev.kavrin.banking_ledger.shared.error.BadRequestException;
import dev.kavrin.banking_ledger.shared.error.ConflictException;
import dev.kavrin.banking_ledger.shared.error.ResourceNotFoundException;
import dev.kavrin.banking_ledger.shared.money.CurrencyCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Application service responsible for creating customer accounts.
 *
 * <p>This use case orchestrates the complete account creation workflow while
 * keeping business rules independent from HTTP and persistence concerns.
 * Its responsibilities include:
 * <ul>
 *   <li>Validating business input.</li>
 *   <li>Loading the target customer.</li>
 *   <li>Preventing duplicate account numbers.</li>
 *   <li>Creating and persisting a new account.</li>
 *   <li>Writing an immutable audit event.</li>
 *   <li>Returning an API response DTO.</li>
 * </ul>
 *
 * <p>The entire workflow executes within a single database transaction so that
 * either both the account and its audit event are committed, or neither is.
 */
@Service
@RequiredArgsConstructor
public class CreateAccountUseCase {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AuditEventWriter auditEventWriter;

    /**
     * Application entry point for creating a new customer account.
     *
     * <p>This method executes within a single database transaction to ensure
     * atomicity of the account creation and audit event writing. The major
     * workflow steps include:
     * <ul>
     *   <li>Normalizing and validating the account number.</li>
     *   <li>Validating the requested account type is allowed for customers.</li>
     *   <li>Loading the customer by ID or throwing if not found.</li>
     *   <li>Checking for duplicate account numbers to prevent conflicts.</li>
     *   <li>Creating and persisting the new account entity.</li>
     *   <li>Writing an immutable audit event recording the creation.</li>
     *   <li>Returning an API response DTO representing the created account.</li>
     * </ul>
     *
     * @param command the command containing all necessary input data to create the account
     * @return the response DTO representing the newly created account
     * @throws BadRequestException if input validation fails
     * @throws ResourceNotFoundException if the customer cannot be found
     * @throws ConflictException if the account number already exists
     */
    @Transactional
    public AccountResponse handle(CreateAccountCommand command) {

        var accountNumber = normalizeAccountNumber(command.accountNumber());

        validateCustomerAccountType(command.accountType());

        var customer = customerRepository.findById(command.customerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                                ApiErrorCode.Business.CUSTOMER_NOT_FOUND,
                                "Customer not found with ID: " + command.customerId(),
                                "Customer not found"
                        ));
        if (accountRepository.existsByAccountNumber(accountNumber)) {
            throw new ConflictException(
                    ApiErrorCode.Business.ACCOUNT_NUMBER_ALREADY_EXISTS,
                    "Account number already exists: " + accountNumber,
                    "Account number already exists."
            );
        }
        var currencyCode = CurrencyCode.of(command.currencyCode().value());
        var accountCategory = resolveCategory(command.accountType());
        var actorType = parseActorType(command.actorType());

        var account = AccountEntity.builder()
                .customer(customer)
                .accountNumber(accountNumber)
                .accountType(command.accountType())
                .accountCategory(accountCategory)
                .status(AccountStatus.ACTIVE)
                .currencyCode(currencyCode.value())
                .availableBalanceMinor(0L)
                .ledgerBalanceMinor(0L)
                .build();

        var savedAccount = accountRepository.save(account);

        auditEventWriter.write(new WriteAuditEventCommand(
                AuditEventType.ACCOUNT_CREATED,
                AuditEntityType.ACCOUNT,
                savedAccount.getId(),
                actorType,
                command.actorRole(),
                command.actorId(),
                command.correlationId(),
                AuditChannel.API,
                new AccountCreatedAuditPayload(savedAccount.getId())
        ));

        return toResponse(savedAccount);

    }

    /**
     * Normalizes and validates the account number string.
     *
     * <p>Normalization trims whitespace and ensures the account number is present
     * and does not exceed the maximum length. This prevents issues with inconsistent
     * formatting and enforces business constraints.
     *
     * @param accountNumber the raw account number input
     * @return the normalized account number
     * @throws BadRequestException if the account number is null, blank, or too long
     */
    private String normalizeAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new BadRequestException(
                    ApiErrorCode.Validation.INVALID_REQUEST,
                    "Account number is required",
                    "Account number is required."
            );
        }

        var normalized = accountNumber.trim();

        if (normalized.length() > 34) {
            throw new BadRequestException(
                    ApiErrorCode.Validation.INVALID_REQUEST,
                    "Account number must be at most 34 characters",
                    "Account number must be at most 34 characters."
            );
        }

        return normalized;
    }

    /**
     * Validates that the given account type is allowed for customer accounts.
     *
     * <p>Customer accounts cannot be created with internal-only account types.
     *
     * @param accountType the account type to validate
     * @throws BadRequestException if the account type is null or internal-only
     */
    private void validateCustomerAccountType(AccountType accountType) {
        if (accountType == null) {
            throw new BadRequestException(
                    ApiErrorCode.Validation.INVALID_REQUEST,
                    "Account type is required",
                    "Account type is required."
            );
        }

        if (resolveCategory(accountType) == AccountCategory.INTERNAL) {
            throw new BadRequestException(
                    ApiErrorCode.Business.INVALID_ACCOUNT_TYPE,
                    "Customer accounts cannot use internal-only account type: " + accountType,
                    "Customer accounts cannot use that account type."
            );
        }
    }

    /**
     * Parses the audit actor type from a string, defaulting to SYSTEM if null or blank.
     *
     * <p>This ensures a valid audit actor type is always recorded for audit events.
     *
     * @param actorType the raw actor type string
     * @return the parsed AuditActorType enum value
     * @throws BadRequestException if the actor type string is invalid
     */
    private AuditActorType parseActorType(String actorType) {
        if (actorType == null || actorType.isBlank()) {
            return AuditActorType.SYSTEM;
        }

        try {
            return AuditActorType.valueOf(actorType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(
                    ApiErrorCode.Validation.INVALID_REQUEST,
                    "Invalid actor type: " + actorType,
                    "Invalid actor type."
            );
        }
    }

    /**
     * Resolves the account category from the given account type.
     *
     * <p>The category is derived by the server and not controlled by the client,
     * enforcing consistent classification rules.
     *
     * @param accountType the account type
     * @return the corresponding account category
     */
    private AccountCategory resolveCategory(AccountType accountType) {
        return switch (accountType) {
            case CURRENT, SAVINGS, WALLET -> AccountCategory.CUSTOMER;
            case SUSPENSE, CLEARING, FEE_INCOME -> AccountCategory.INTERNAL;
        };

    }

    /**
     * Maps the persistence AccountEntity to an API response DTO.
     *
     * <p>This prevents the persistence model from leaking through the API layer,
     * providing a clean separation of concerns.
     *
     * @param account the persisted account entity
     * @return the API response DTO representing the account
     */
    private AccountResponse toResponse(AccountEntity account) {
        return new AccountResponse(
                account.getId(),
                account.getCustomer().getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getAccountCategory(),
                account.getStatus(),
                account.getCurrencyCode(),
                account.getAvailableBalanceMinor(),
                account.getLedgerBalanceMinor(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );

    }
}
