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

import customer.api.CustomerEventsServiceAction;
import kalix.javasdk.Kalix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import customer.domain.CustomerEntity;
import customer.view.CustomerByNameView;

public final class Main {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  // tag::register[]
  public static Kalix createKalix() {
    return KalixFactory.withComponents(
      CustomerEntity::new,
      CustomerByNameView::new,
      CustomerEventsServiceAction::new
    );
  }
  // end::register[]

  public static void main(String[] args) throws Exception {
    LOG.info("starting the Kalix service");
    createKalix().start();
  }
}
