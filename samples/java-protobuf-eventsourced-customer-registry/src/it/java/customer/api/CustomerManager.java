package customer.api;

import com.google.protobuf.Empty;

import java.util.concurrent.*;
import java.util.function.Supplier;

public class CustomerManager {
    private final CustomerService client;
    private static final int DEFAULT_TIMEOUT_SECONDS = 5;

    public CustomerManager(CustomerService client) {
        this.client = client;
    }

    /**
     * Creates a customer and updates their name in sequence
     *
     * @param customerId The customer ID
     * @param initialName The initial name for the customer
     * @param email The customer's email
     * @param updatedName The new name to change to
     * @return The updated customer information
     * @throws CustomerOperationException if any operation fails
     */
    public Empty createAndUpdateCustomer(
            String customerId,
            String initialName,
            String email,
            String updatedName) throws CustomerOperationException {

        // Create the customer first
        createCustomer(customerId, initialName, email);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // Then update their name
        return updateCustomerName(customerId, updatedName);
    }

    /**
     * Creates a new customer
     */
    public Empty createCustomer(String customerId, String name, String email)
            throws CustomerOperationException {
        CustomerApi.Customer customer = CustomerApi.Customer.newBuilder()
                .setCustomerId(customerId)
                .setName(name)
                .setEmail(email)
                .build();

        return executeWithTimeout(() -> client.create(customer), "Customer creation failed");
    }

    /**
     * Updates a customer's name
     */
    public Empty updateCustomerName(String customerId, String newName)
            throws CustomerOperationException {
        CustomerApi.ChangeNameRequest changeNameRequest = CustomerApi.ChangeNameRequest.newBuilder()
                .setCustomerId(customerId)
                .setNewName(newName)
                .build();

        return executeWithTimeout(() -> client.changeName(changeNameRequest),
                "Customer name update failed for ID: " + customerId);
    }

    /**
     * Helper method to execute API calls with proper timeout and error handling
     */
    private <T> T executeWithTimeout(Supplier<CompletionStage<T>> operation, String errorMessage)
            throws CustomerOperationException {
        try {
            return operation.get().toCompletableFuture()
                    .get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomerOperationException(errorMessage + ": Operation was interrupted", e);
        } catch (ExecutionException e) {
            throw new CustomerOperationException(errorMessage + ": " + e.getCause().getMessage(), e.getCause());
        } catch (TimeoutException e) {
            throw new CustomerOperationException(errorMessage + ": Operation timed out after "
                    + DEFAULT_TIMEOUT_SECONDS + " seconds", e);
        }
    }

    /**
     * Custom exception for customer operations
     */
    public static class CustomerOperationException extends Exception {
        public CustomerOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}