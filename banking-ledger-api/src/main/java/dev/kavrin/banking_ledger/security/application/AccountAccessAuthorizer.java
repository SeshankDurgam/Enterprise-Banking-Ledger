package dev.kavrin.banking_ledger.security.application;

import dev.kavrin.banking_ledger.account.persistence.AccountRepository;
import dev.kavrin.banking_ledger.security.domain.AuthenticatedPrincipal;
import dev.kavrin.banking_ledger.security.domain.SecurityRole;
import dev.kavrin.banking_ledger.transfer.persistence.TransferRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Centralizes resource-based authorization rules for accounts and transfers.
 *
 * <p>This component is referenced from Spring Security {@code @PreAuthorize}
 * expressions using the bean name {@code accountOwnership}. Rather than relying
 * only on roles, it answers questions such as whether the authenticated user is
 * allowed to access a specific account or transfer.
 *
 * <p>Typical rules implemented here are:
 * <ul>
 *   <li>Back-office roles (TELLER, AUDITOR, OPS_ADMIN) may access any account.</li>
 *   <li>Customers may only access resources that belong to them.</li>
 *   <li>Service accounts may read transfers when required for internal processing.</li>
 * </ul>
 *
 * <p>Keeping these rules in a dedicated bean avoids duplicating authorization
 * logic across controllers and services while keeping {@code @PreAuthorize}
 * expressions concise.
 */
@Component("accountOwnership")
@RequiredArgsConstructor
public class AccountAccessAuthorizer {

    private final AccountRepository accountRepository;
    private final TransferRequestRepository transferRequestRepository;

    /**
     * Determines if the authenticated principal is authorized to read the account with the given ID.
     *
     * @param principal the authenticated user principal
     * @param accountId the UUID of the account to read
     * @return {@code true} if the principal has back-office read roles or owns the account; {@code false} otherwise
     */
    public boolean canReadAccount(AuthenticatedPrincipal principal, UUID accountId) {
        return hasBackOfficeRead(principal) || ownsAccount(principal, accountId);
    }

    /**
     * Determines if the authenticated principal is authorized to read the account with the given account number.
     *
     * @param principal     the authenticated user principal
     * @param accountNumber the account number to read
     * @return {@code true} if the principal has back-office read roles or owns the account number; {@code false} otherwise
     */
    public boolean canReadAccountNumber(AuthenticatedPrincipal principal, String accountNumber) {
        return hasBackOfficeRead(principal) || ownsAccountNumber(principal, accountNumber);
    }

    /**
     * Determines if the authenticated principal is authorized to create a transfer from the specified source account.
     *
     * @param principal       the authenticated user principal
     * @param sourceAccountId the UUID of the source account for the transfer
     * @return {@code true} if the principal has teller or ops admin roles or owns the source account; {@code false} otherwise
     */
    public boolean canCreateTransfer(AuthenticatedPrincipal principal, UUID sourceAccountId) {
        return principal.hasRole(SecurityRole.TELLER)
                || principal.hasRole(SecurityRole.OPS_ADMIN)
                || ownsAccount(principal, sourceAccountId);
    }

    /**
     * Determines if the authenticated principal is authorized to read the transfer with the given transfer ID.
     *
     * @param principal  the authenticated user principal
     * @param transferId the UUID of the transfer to read
     * @return {@code true} if the principal has back-office read roles, is a service account,
     * or if the transfer belongs to the principal's customer; {@code false} otherwise
     */
    public boolean canReadTransfer(AuthenticatedPrincipal principal, UUID transferId) {
        return hasBackOfficeRead(principal)
                || principal.hasRole(SecurityRole.SERVICE)
                || (principal.customerId() != null
                && transferRequestRepository.existsByIdAndCustomerId(transferId, principal.customerId()));
    }

    /**
     * Checks if the principal has any back-office roles that grant read access.
     *
     * @param principal the authenticated user principal
     * @return {@code true} if the principal has TELLER, AUDITOR, or OPS_ADMIN roles; {@code false} otherwise
     */
    private boolean hasBackOfficeRead(AuthenticatedPrincipal principal) {
        return principal.hasRole(SecurityRole.TELLER)
                || principal.hasRole(SecurityRole.AUDITOR)
                || principal.hasRole(SecurityRole.OPS_ADMIN);
    }

    /**
     * Checks if the principal owns the specified account by ID.
     *
     * @param principal the authenticated user principal
     * @param accountId the UUID of the account
     * @return {@code true} if the principal's customer ID matches the account's customer ID; {@code false} otherwise
     */
    private boolean ownsAccount(AuthenticatedPrincipal principal, UUID accountId) {
        return principal.customerId() != null
                && accountRepository.existsByIdAndCustomer_Id(accountId, principal.customerId());
    }

    /**
     * Checks if the principal owns the specified account by account number.
     *
     * @param principal     the authenticated user principal
     * @param accountNumber the account number
     * @return {@code true} if the principal's customer ID matches the account's customer ID; {@code false} otherwise
     */
    private boolean ownsAccountNumber(AuthenticatedPrincipal principal, String accountNumber) {
        return principal.customerId() != null
                && accountRepository.existsByAccountNumberAndCustomer_Id(accountNumber, principal.customerId());
    }
}
