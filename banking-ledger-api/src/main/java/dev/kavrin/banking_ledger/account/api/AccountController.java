package dev.kavrin.banking_ledger.account.api;

import dev.kavrin.banking_ledger.account.api.dto.AccountResponse;
import dev.kavrin.banking_ledger.account.api.dto.AccountTransactionSummaryResponse;
import dev.kavrin.banking_ledger.account.api.dto.BalanceResponse;
import dev.kavrin.banking_ledger.account.api.dto.CreateAccountRequest;
import dev.kavrin.banking_ledger.account.application.command.CreateAccountCommand;
import dev.kavrin.banking_ledger.account.application.query.GetAccountBalanceQuery;
import dev.kavrin.banking_ledger.account.application.query.GetAccountByIdQuery;
import dev.kavrin.banking_ledger.account.application.query.GetAccountByNumberQuery;
import dev.kavrin.banking_ledger.account.application.query.GetAccountTransactionsQuery;
import dev.kavrin.banking_ledger.account.application.service.AccountQueryUseCase;
import dev.kavrin.banking_ledger.account.application.service.CreateAccountUseCase;
import dev.kavrin.banking_ledger.account.application.service.GetAccountBalanceUseCase;
import dev.kavrin.banking_ledger.account.application.service.GetAccountTransactionsUseCase;
import dev.kavrin.banking_ledger.security.domain.AuthenticatedPrincipal;
import dev.kavrin.banking_ledger.shared.money.CurrencyCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * REST endpoints for account creation and account queries.
 *
 * <p>This controller demonstrates two authorization strategies:
 * <ul>
 *   <li><b>Role-based authorization</b> using {@code hasRole()} or {@code hasAnyRole()} for operational actions.</li>
 *   <li><b>Resource-based authorization</b> using the {@code accountOwnership} authorizer to ensure customers can only access their own resources.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final AccountQueryUseCase accountQueryUseCase;
    private final GetAccountBalanceUseCase getAccountBalanceUseCase;
    private final GetAccountTransactionsUseCase getAccountTransactionsUseCase;

    /**
     * Creates a new account for a customer.
     *
     * <p>This endpoint uses role-based authorization because creating accounts is an operational action.
     * Only users with roles TELLER or OPS_ADMIN are allowed to create accounts.
     *
     * @param request the account creation request containing customer ID, account number, type, and currency code
     * @param principal the authenticated principal performing the operation
     * @param correlationId optional correlation ID for tracing
     * @return a response entity containing the created account details and location URI
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TELLER', 'OPS_ADMIN')")
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        var response = createAccountUseCase.handle(new CreateAccountCommand(
                request.customerId(),
                request.accountNumber(),
                request.accountType(),
                CurrencyCode.of(request.currencyCode()),
                principal.actorType().name(),
                principal.auditActorRole(),
                principal.actorId(),
                correlationId
        ));

        return ResponseEntity
                .created(URI.create("/api/v1/accounts/" + response.id()))
                .body(response);
    }

    /**
     * Retrieves account details by account ID.
     *
     * <p>This endpoint uses resource-based authorization to ensure that customers can only access their own accounts,
     * while back-office roles or auditors may have broader access.
     * The {@code accountOwnership} bean is referenced in the {@code @PreAuthorize} expression and evaluated before method execution.
     *
     * @param accountId the UUID of the account to retrieve
     * @return the account details
     */
    @GetMapping("/{accountId}")
    @PreAuthorize("@accountOwnership.canReadAccount(authentication.principal, #accountId)")
    public AccountResponse getById(@PathVariable UUID accountId) {
        return accountQueryUseCase.getById(new GetAccountByIdQuery(accountId));
    }

    /**
     * Retrieves account details by account number.
     *
     * <p>This endpoint uses resource-based authorization to ensure that customers can only access their own accounts by number,
     * with back-office roles having broader access.
     * The {@code accountOwnership} bean is referenced in the {@code @PreAuthorize} expression and evaluated before method execution.
     *
     * @param accountNumber the account number to retrieve
     * @return the account details
     */
    @GetMapping("/by-number/{accountNumber}")
    @PreAuthorize("@accountOwnership.canReadAccountNumber(authentication.principal, #accountNumber)")
    public AccountResponse getByNumber(@PathVariable String accountNumber) {
        return accountQueryUseCase.getByNumber(new GetAccountByNumberQuery(accountNumber));
    }

    /**
     * Retrieves the balance of an account.
     *
     * <p>This endpoint uses resource-based authorization to ensure that only authorized users can access the balance.
     * The {@code accountOwnership} bean is referenced in the {@code @PreAuthorize} expression and evaluated before method execution.
     *
     * @param accountId the UUID of the account whose balance is requested
     * @return the balance response containing current account balance
     */
    @GetMapping("/{accountId}/balance")
    @PreAuthorize("@accountOwnership.canReadAccount(authentication.principal, #accountId)")
    public BalanceResponse getBalance(@PathVariable UUID accountId) {
        return getAccountBalanceUseCase.handle(new GetAccountBalanceQuery(accountId));
    }

    /**
     * Retrieves a paginated list of transactions for an account within an optional date range.
     *
     * <p>This endpoint uses resource-based authorization to ensure that only authorized users can access transactions.
     * The {@code accountOwnership} bean is referenced in the {@code @PreAuthorize} expression and evaluated before method execution.
     *
     * @param accountId the UUID of the account whose transactions are requested
     * @param from optional start datetime filter (inclusive)
     * @param to optional end datetime filter (inclusive)
     * @param page the page number to retrieve (zero-based)
     * @param size the number of transactions per page
     * @return a page of account transaction summary responses
     */
    @GetMapping("/{accountId}/transactions")
    @PreAuthorize("@accountOwnership.canReadAccount(authentication.principal, #accountId)")
    public Page<AccountTransactionSummaryResponse> getTransactions(
            @PathVariable UUID accountId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return getAccountTransactionsUseCase.handle(new GetAccountTransactionsQuery(
                accountId,
                from,
                to,
                page,
                size
        ));
    }
}
