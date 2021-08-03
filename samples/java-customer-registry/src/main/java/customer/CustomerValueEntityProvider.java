/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package customer;

import com.akkaserverless.javasdk.valueentity.ValueEntityContext;
import com.akkaserverless.javasdk.valueentity.ValueEntityProvider;
import com.google.protobuf.Descriptors;
import customer.api.CustomerApi;

import java.util.function.Function;

// to be generated in generated-sources/src/main/java
public class CustomerValueEntityProvider implements ValueEntityProvider {

  private final Function<ValueEntityContext, CustomerValueEntity> entityProviderFunc;

  public CustomerValueEntityProvider(
      Function<ValueEntityContext, CustomerValueEntity> entityProviderFunc) {
    this.entityProviderFunc = entityProviderFunc;
  }

  // depends on proto file definition therefore final
  @Override
  public final Descriptors.ServiceDescriptor serviceDescriptor() {
    return CustomerApi.getDescriptor().findServiceByName("CustomerService");
  }

  // depends on proto file definition therefore final
  @Override
  public final String entityTypeHint() {
    return "customers";
  }

  // depends on proto file definition therefore final
  @Override
  public final CustomerValueEntityHandler newHandler(ValueEntityContext context) {
    return new CustomerValueEntityHandler(entityProviderFunc.apply(context));
  }

  // depends on proto file definition therefore final
  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[0];
  }
}
