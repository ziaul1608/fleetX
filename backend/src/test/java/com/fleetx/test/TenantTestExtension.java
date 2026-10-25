package com.fleetx.test;

import com.fleetx.shared.context.TenantSecurityContext;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that automatically establishes an explicit test tenant context (Tenant 1)
 * before each test method and guarantees clean teardown after each test.
 *
 * Specific tests that need to test unauthenticated, missing-context, or multi-tenant scenarios
 * can explicitly call {@link TenantSecurityContext#clear()} or {@link TenantSecurityContext#setTenantId(Long)}
 * within the test body.
 */
public class TenantTestExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        TenantSecurityContext.setTenantId(1L);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        TenantSecurityContext.clear();
    }
}
