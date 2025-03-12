/*
 *    Copyright (c) 2025, VRAI Labs and/or its affiliates. All rights reserved.
 *
 *    This software is licensed under the Apache License, Version 2.0 (the
 *    "License") as published by the Apache Software Foundation.
 *
 *    You may not use this file except in compliance with the License. You may
 *    obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 */

package io.supertokens.test.webauthn.api;

import com.google.gson.JsonObject;
import io.supertokens.ProcessState;
import io.supertokens.emailpassword.EmailPassword;
import io.supertokens.featureflag.EE_FEATURES;
import io.supertokens.featureflag.FeatureFlagTestContent;
import io.supertokens.pluginInterface.STORAGE_TYPE;
import io.supertokens.pluginInterface.authRecipe.AuthRecipeUserInfo;
import io.supertokens.storageLayer.StorageLayer;
import io.supertokens.test.TestingProcessManager;
import io.supertokens.test.Utils;
import io.supertokens.test.httpRequest.HttpRequestForTesting;
import io.supertokens.test.httpRequest.HttpResponseException;
import io.supertokens.utils.SemVer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class TestGetUserFromRecoverAccountTokenAPI_5_3 {
    @Rule
    public TestRule watchman = Utils.getOnFailure();

    @AfterClass
    public static void afterTesting() {
        Utils.afterTesting();
    }

    @Before
    public void beforeEach() {
        Utils.reset();
    }
    
    @Test
    public void testInvalidInput() throws Exception {
        String[] args = {"../"};

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        if (StorageLayer.getStorage(process.getProcess()).getType() != STORAGE_TYPE.SQL) {
            return;
        }

        Map<String, String> params = new HashMap<>();
        try {
            JsonObject resp = HttpRequestForTesting.sendGETRequest(process.getProcess(), "",
                    "http://localhost:3567/recipe/webauthn/user/recover", params, 1000, 1000, null,
                    SemVer.v5_3.get(), "webauthn");
            fail();
        } catch (HttpResponseException e) {
            assertEquals(400, e.statusCode);
            assertEquals("Http error. Status Code: 400. Message: Field name 'token' is missing in GET request", e.getMessage());
        }

        params.put("token", "abcd");
        try {
            JsonObject resp = HttpRequestForTesting.sendGETRequest(process.getProcess(), "",
                    "http://localhost:3567/recipe/webauthn/user/recover", params, 1000, 1000, null,
                    SemVer.v5_3.get(), "webauthn");
        } catch (HttpResponseException e) {
            fail(e.getMessage());
        }

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    @Test
    public void testInvalidToken() throws Exception {
        String[] args = {"../"};

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        if (StorageLayer.getStorage(process.getProcess()).getType() != STORAGE_TYPE.SQL) {
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("token", "abcd");
        try {
            JsonObject resp = HttpRequestForTesting.sendGETRequest(process.getProcess(), "",
                    "http://localhost:3567/recipe/webauthn/user/recover", params, 1000, 1000, null,
                    SemVer.v5_3.get(), "webauthn");
            assertEquals("RECOVER_ACCOUNT_TOKEN_INVALID_ERROR", resp.get("status").getAsString());
        } catch (HttpResponseException e) {
            fail(e.getMessage());
        }

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    @Test
    public void testValidInput() throws Exception {
        String[] args = {"../"};

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args, false);
        FeatureFlagTestContent.getInstance(process.getProcess())
                .setKeyValue(FeatureFlagTestContent.ENABLED_FEATURES, new EE_FEATURES[]{
                        EE_FEATURES.ACCOUNT_LINKING, EE_FEATURES.MULTI_TENANCY});
        process.startProcess();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        if (StorageLayer.getStorage(process.getProcess()).getType() != STORAGE_TYPE.SQL) {
            return;
        }

        String token = null;
        { // Generate
            AuthRecipeUserInfo user = EmailPassword.signUp(process.getProcess(),
                    "test@example.com", "password123");

            JsonObject req = new JsonObject();
            req.addProperty("email", "test@example.com");
            req.addProperty("userId", user.getSupertokensUserId());

            try {
                JsonObject resp = HttpRequestForTesting.sendJsonPOSTRequest(process.getProcess(), "",
                        "http://localhost:3567/recipe/webauthn/user/recover/token", req, 1000, 1000, null,
                        SemVer.v5_3.get(), "webauthn");
                token = resp.get("token").getAsString();
            } catch (HttpResponseException e) {
                fail(e.getMessage());
            }
        }

        Map<String, String> params = new HashMap<>();
        params.put("token", token);
        try {
            JsonObject resp = HttpRequestForTesting.sendGETRequest(process.getProcess(), "",
                    "http://localhost:3567/recipe/webauthn/user/recover", params, 1000, 1000, null,
                    SemVer.v5_3.get(), "webauthn");
            checkResponseStructure(resp);
        } catch (HttpResponseException e) {
            fail(e.getMessage());
        }

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    private void checkResponseStructure(JsonObject resp) throws Exception {
        assertEquals("OK", resp.get("status").getAsString());

        assertEquals(3, resp.entrySet().size());

        assertTrue(resp.has("user"));
        JsonObject user = resp.get("user").getAsJsonObject();
        assertEquals(9, user.entrySet().size());
        assertTrue(user.has("id"));
        assertTrue(user.has("isPrimaryUser"));
        assertTrue(user.has("tenantIds"));
        assertTrue(user.has("timeJoined"));
        assertTrue(user.has("emails"));
        assertTrue(user.has("phoneNumbers"));
        assertTrue(user.has("thirdParty"));
        assertTrue(user.has("webauthn"));

        JsonObject webauthn = user.get("webauthn").getAsJsonObject();
        assertEquals(1, webauthn.entrySet().size());
        assertTrue(webauthn.has("credentialIds"));
        assertTrue(webauthn.get("credentialIds").isJsonArray());

        assertTrue(user.has("loginMethods"));

        assertTrue(resp.has("recipeUserId"));
    }
}
