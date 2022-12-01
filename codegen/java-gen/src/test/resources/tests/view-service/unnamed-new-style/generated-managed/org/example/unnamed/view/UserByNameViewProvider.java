package org.example.unnamed.view;

import com.google.protobuf.Descriptors;
import kalix.javasdk.view.ViewCreationContext;
import kalix.javasdk.view.ViewOptions;
import kalix.javasdk.view.ViewProvider;

import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public class UserByNameViewProvider implements ViewProvider {

  private final Function<ViewCreationContext, UserByNameView> viewFactory;
  private final String viewId;
  private final ViewOptions options;

  /** Factory method of UserByNameView */
  public static UserByNameViewProvider of(
      Function<ViewCreationContext, UserByNameView> viewFactory) {
    return new UserByNameViewProvider(viewFactory, "UserByName", ViewOptions.defaults());
  }

  private UserByNameViewProvider(
      Function<ViewCreationContext, UserByNameView> viewFactory,
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

  public final UserByNameViewProvider withOptions(ViewOptions options) {
    return new UserByNameViewProvider(viewFactory, viewId, options);
  }

  /**
   * Use a custom view identifier. By default, the viewId is the same as the proto service name.
   * A different identifier can be needed when making rolling updates with changes to the view definition.
   */
  public UserByNameViewProvider withViewId(String viewId) {
    return new UserByNameViewProvider(viewFactory, viewId, options);
  }

  @Override
  public final Descriptors.ServiceDescriptor serviceDescriptor() {
    return UserViewModel.getDescriptor().findServiceByName("UserByName");
  }

  @Override
  public final UserByNameViewRouter newRouter(ViewCreationContext context) {
    return new UserByNameViewRouter(viewFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {UserViewModel.getDescriptor()};
  }
}

