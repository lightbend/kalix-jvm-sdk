package org.example.view;

import com.google.protobuf.Descriptors;
import kalix.javasdk.view.ViewCreationContext;
import kalix.javasdk.view.ViewOptions;
import kalix.javasdk.view.ViewProvider;

import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public class CustomerOrdersViewProvider implements ViewProvider {

  private final Function<ViewCreationContext, CustomerOrdersView> viewFactory;
  private final String viewId;
  private final ViewOptions options;

  /** Factory method of CustomerOrdersView */
  public static CustomerOrdersViewProvider of(
      Function<ViewCreationContext, CustomerOrdersView> viewFactory) {
    return new CustomerOrdersViewProvider(viewFactory, "CustomerOrders", ViewOptions.defaults());
  }

  private CustomerOrdersViewProvider(
      Function<ViewCreationContext, CustomerOrdersView> viewFactory,
      String viewId,
      ViewOptions options) {
    this.viewFactory = viewFactory;
    this.viewId = viewId;
    this.options = options;
  }

  @Override
  public String viewId() {
    return viewId;
  }

  @Override
  public final ViewOptions options() {
    return options;
  }

  public final CustomerOrdersViewProvider withOptions(ViewOptions options) {
    return new CustomerOrdersViewProvider(viewFactory, viewId, options);
  }

  /**
   * Use a custom view identifier. By default, the viewId is the same as the proto service name.
   * A different identifier can be needed when making rolling updates with changes to the view definition.
   */
  public CustomerOrdersViewProvider withViewId(String viewId) {
    return new CustomerOrdersViewProvider(viewFactory, viewId, options);
  }

  @Override
  public final Descriptors.ServiceDescriptor serviceDescriptor() {
    return CustomerOrdersViewModel.getDescriptor().findServiceByName("CustomerOrders");
  }

  @Override
  public final CustomerOrdersViewRouter newRouter(ViewCreationContext context) {
    return new CustomerOrdersViewRouter(viewFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {CustomerOrdersViewModel.getDescriptor()};
  }
}

