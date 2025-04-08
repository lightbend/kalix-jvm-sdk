package customer.api;

import java.util.List;

public class CustomerBatchProcessor {
    private final CustomerService client;
    private final CustomerManager manager;

    public CustomerBatchProcessor(CustomerService client) {
        this.client = client;
        this.manager = new CustomerManager(client);
    }


    public void processBatch(List<CustomerData> customerDataList) {
        customerDataList.forEach(customerData -> {
            try {
                manager.createAndUpdateCustomer(
                        customerData.getCustomerId(),
                        customerData.getInitialName(),
                        customerData.getEmail(),
                        customerData.getUpdatedName());
            } catch (CustomerManager.CustomerOperationException e) {
                throw new RuntimeException(e);
            }
        });

    }


    /**
     * Customer data class
     */
    public static class CustomerData {
        private String customerId;
        private String initialName;
        private String email;
        private String updatedName;

        // Getters and setters
        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public String getInitialName() {
            return initialName;
        }

        public void setInitialName(String initialName) {
            this.initialName = initialName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getUpdatedName() {
            return updatedName;
        }

        public void setUpdatedName(String updatedName) {
            this.updatedName = updatedName;
        }

    }

}