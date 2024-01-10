/*
 * Copyright 2024 Lightbend Inc.
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

package com.example.wiring.valueentities.user;

import java.util.concurrent.ConcurrentHashMap;

public class UserSideEffect {

  static final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();

  static void addUser(String id, User user){
    users.put(id, user);
  }

  static void removeUser(String id){
    users.remove(id);
  }

  public static ConcurrentHashMap<String, User> getUsers() {
    return users;
  }
}
