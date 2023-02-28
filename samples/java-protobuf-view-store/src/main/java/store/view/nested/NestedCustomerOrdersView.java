package store.view.nested;

import kalix.javasdk.view.View;
import kalix.javasdk.view.ViewContext;
import store.customer.domain.CustomerDomain;
import store.product.domain.ProductDomain;

public class NestedCustomerOrdersView extends AbstractNestedCustomerOrdersView {

  public NestedCustomerOrdersView(ViewContext context) {
    super(context);
  }

  @Override
  public CustomersViewTable createCustomersViewTable(ViewContext context) {
    return new CustomersViewTable(context);
  }

  public static class CustomersViewTable extends AbstractCustomersViewTable {

    public CustomersViewTable(ViewContext context) {}

    @Override
    public CustomerDomain.CustomerState emptyState() {
      return CustomerDomain.CustomerState.getDefaultInstance();
    }

    @Override
    public View.UpdateEffect<CustomerDomain.CustomerState> processCustomerCreated(
        CustomerDomain.CustomerState state, CustomerDomain.CustomerCreated customerCreated) {
      return !state.getCustomerId().isEmpty()
          ? effects().ignore() // already created
          : effects().updateState(customerCreated.getCustomer());
    }

    @Override
    public View.UpdateEffect<CustomerDomain.CustomerState> processCustomerNameChanged(
        CustomerDomain.CustomerState state,
        CustomerDomain.CustomerNameChanged customerNameChanged) {
      return effects()
          .updateState(state.toBuilder().setName(customerNameChanged.getNewName()).build());
    }

    @Override
    public View.UpdateEffect<CustomerDomain.CustomerState> processCustomerAddressChanged(
        CustomerDomain.CustomerState state,
        CustomerDomain.CustomerAddressChanged customerAddressChanged) {
      return effects()
          .updateState(
              state.toBuilder().setAddress(customerAddressChanged.getNewAddress()).build());
    }
  }

  @Override
  public ProductsViewTable createProductsViewTable(ViewContext context) {
    return new ProductsViewTable(context);
  }

  public static class ProductsViewTable extends AbstractProductsViewTable {

    public ProductsViewTable(ViewContext context) {}

    @Override
    public ProductDomain.ProductState emptyState() {
      return ProductDomain.ProductState.getDefaultInstance();
    }

    @Override
    public View.UpdateEffect<ProductDomain.ProductState> processProductCreated(
        ProductDomain.ProductState state, ProductDomain.ProductCreated productCreated) {
      return !state.getProductId().isEmpty()
          ? effects().ignore() // already created
          : effects().updateState(productCreated.getProduct());
    }

    @Override
    public View.UpdateEffect<ProductDomain.ProductState> processProductNameChanged(
        ProductDomain.ProductState state, ProductDomain.ProductNameChanged productNameChanged) {
      return effects()
          .updateState(state.toBuilder().setProductName(productNameChanged.getNewName()).build());
    }

    @Override
    public View.UpdateEffect<ProductDomain.ProductState> processProductPriceChanged(
        ProductDomain.ProductState state, ProductDomain.ProductPriceChanged productPriceChanged) {
      return effects()
          .updateState(state.toBuilder().setPrice(productPriceChanged.getNewPrice()).build());
    }
  }
}
