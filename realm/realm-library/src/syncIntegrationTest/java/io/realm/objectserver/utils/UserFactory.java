/*
 * Copyright 2016 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm.objectserver.utils;

import java.math.BigInteger;
import java.security.SecureRandom;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.SyncCredentials;
import io.realm.SyncUser;
import io.realm.log.RealmLog;

// Must be in `io.realm.objectserver` to work around package protected methods.
public class UserFactory {
    private static final String PASSWORD = "myPassw0rd";
    // Since the integration tests need use the same user for different processes, we create a new user name when the
    // test starts and store it in a Realm. Then it can be retrieved for every process.
    private String userName;
    private static UserFactory instance;
    private static RealmConfiguration configuration = new RealmConfiguration.Builder()
            .name("user-factory.realm")
            .build();

    private UserFactory(String userName) {
        this.userName = userName;
    }

    public SyncUser loginWithDefaultUser(String authUrl) {
        SyncCredentials credentials = SyncCredentials.usernamePassword(userName, PASSWORD, false);
        return SyncUser.login(credentials, authUrl);
    }

    public SyncUser createDefaultUser(String authUrl) {
        SyncCredentials credentials = SyncCredentials.usernamePassword(userName, PASSWORD, true);
        RealmLog.error("TTT userName " + userName);
        return SyncUser.login(credentials, authUrl);
    }

    public static SyncUser createAdminUser(String authUrl) {
        // `admin` required as user identifier to be granted admin rights.
        SyncCredentials credentials = SyncCredentials.custom("admin", "debug", null);
        return SyncUser.login(credentials, authUrl);
    }

    // Since we don't have a reliable way to reset the sync server and client, just use a new user for every test case.
    public static void resetInstance() {
        instance = null;
        SecureRandom random = new SecureRandom();
        Realm realm = Realm.getInstance(configuration);
        UserFactoryStore store = realm.where(UserFactoryStore.class).findFirst();
        realm.beginTransaction();
        if (store == null) {
            store = realm.createObject(UserFactoryStore.class);
        }
        store.setUserName(new BigInteger(130, random).toString(32));
        realm.commitTransaction();
        realm.close();
    }

    // The @Before method will be called before the looper tests finished. We need to find a better place to call this.
    public static void clearInstance()  {
        Realm realm = Realm.getInstance(configuration);
        realm.beginTransaction();
        realm.delete(UserFactoryStore.class);
        realm.commitTransaction();
        realm.close();
    }

    public static UserFactory getInstance() {
        if (instance == null)  {
            Realm realm = Realm.getInstance(configuration);
            RealmLog.error("TTT " + realm.getPath());
            UserFactoryStore store = realm.where(UserFactoryStore.class).findFirst();
            if (store == null || store.getUserName() == null) {
                throw new IllegalStateException("Current user has not been set. Call resetInstance() first.");
            }

            instance = new UserFactory(store.getUserName());
            realm.close();
        }
        return instance;
    }
}