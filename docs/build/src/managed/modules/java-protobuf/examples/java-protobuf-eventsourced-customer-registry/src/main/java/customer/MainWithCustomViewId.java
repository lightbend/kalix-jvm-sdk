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

import kalix.javasdk.Kalix;
import customer.domain.CustomerEntity;
import customer.domain.CustomerEntityProvider;
import customer.view.CustomerByNameView;
import customer.view.CustomerByNameViewProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MainWithCustomViewId {

  private static final Logger LOG = LoggerFactory.getLogger(MainWithCustomViewId.class);

  // tag::register[]
  public static Kalix createKalix() {
    Kalix kalix = new Kalix();
    kalix.register(CustomerByNameViewProvider.of(CustomerByNameView::new)
        .withViewId("CustomerByNameV2"));
    kalix.register(CustomerEntityProvider.of(CustomerEntity::new));
    return kalix;
  }
  // end::register[]

  public static void main(String[] args) throws Exception {
    LOG.info("starting the Kalix service");
    createKalix().start();
  }
}
