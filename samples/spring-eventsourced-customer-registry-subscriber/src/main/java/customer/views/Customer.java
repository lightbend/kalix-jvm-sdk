package customer.views;

public record Customer(String id, String email, String name) {
  public Customer withName(String newName) {
    return new Customer(id, email, newName);
  }
}
