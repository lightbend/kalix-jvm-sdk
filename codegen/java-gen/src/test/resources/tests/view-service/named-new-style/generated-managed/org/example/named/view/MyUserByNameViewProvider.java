package org.example.named.view;

import com.google.protobuf.Descriptors;
import kalix.javasdk.view.ViewCreationContext;
import kalix.javasdk.view.ViewOptions;
import kalix.javasdk.view.ViewProvider;

import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public class MyUserByNameViewProvider implements ViewProvider {

  private final Function<ViewCreationContext, MyUserByNameView> viewFactory;
  private final String viewId;
  private final ViewOptions options;

  /** Factory method of MyUserByNameView */
  public static MyUserByNameViewProvider of(
      Function<ViewCreationContext, MyUserByNameView> viewFactory) {
    return new MyUserByNameViewProvider(viewFactory, "UserByName", ViewOptions.defaults());
  }

  private MyUserByNameViewProvider(
      Function<ViewCreationContext, MyUserByNameView> viewFactory,
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

  public final MyUserByNameViewProvider withOptions(ViewOptions options) {
    return new MyUserByNameViewProvider(viewFactory, viewId, options);
  }

  /**
   * Use a custom view identifier. By default, the viewId is the same as the proto service name.
   * A different identifier can be needed when making rolling updates with changes to the view definition.
   */
  public MyUserByNameViewProvider withViewId(String viewId) {
    return new MyUserByNameViewProvider(viewFactory, viewId, options);
  }

  @Override
  public final Descriptors.ServiceDescriptor serviceDescriptor() {
    return UserViewModel.getDescriptor().findServiceByName("UserByName");
  }

  @Override
  public final MyUserByNameViewRouter newRouter(ViewCreationContext context) {
    return new MyUserByNameViewRouter(viewFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {UserViewModel.getDescriptor()};
  }
}

