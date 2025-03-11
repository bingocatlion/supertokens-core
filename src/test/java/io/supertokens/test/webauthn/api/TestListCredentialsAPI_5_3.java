package io.supertokens.test.webauthn.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.webauthn4j.data.AuthenticatorAttestationResponse;
import com.webauthn4j.data.PublicKeyCredential;
import com.webauthn4j.data.PublicKeyCredentialCreationOptions;
import com.webauthn4j.data.extension.client.RegistrationExtensionClientOutput;
import com.webauthn4j.test.EmulatorUtil;
import com.webauthn4j.test.client.ClientPlatform;
import com.webauthn4j.util.Base64UrlUtil;
import io.supertokens.ProcessState;
import io.supertokens.pluginInterface.STORAGE_TYPE;
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

public class TestListCredentialsAPI_5_3 {
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
                    "http://localhost:3567/recipe/webauthn/user/credential/list", params, 1000, 1000, null,
                    SemVer.v5_3.get(), "webauthn");
            fail();
        } catch (HttpResponseException e) {
            assertEquals(400, e.statusCode);
            assertEquals("Http error. Status Code: 400. Message: Field name 'recipeUserId' is missing in GET request", e.getMessage());
        }

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    @Test
    public void testNonExistantUserId() throws Exception {
        String[] args = {"../"};

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        if (StorageLayer.getStorage(process.getProcess()).getType() != STORAGE_TYPE.SQL) {
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("recipeUserId", "nonExistantId");

        try {
            JsonObject resp = HttpRequestForTesting.sendGETRequest(process.getProcess(), "",
                    "http://localhost:3567/recipe/webauthn/user/credential/list", params, 1000, 1000, null,
                    SemVer.v5_3.get(), "webauthn");
            assertEquals("OK", resp.get("status").getAsString());
            assertEquals(0, resp.get("credentials").getAsJsonArray().size());
            assertEquals(2, resp.entrySet().size());
        } catch (HttpResponseException e) {
            fail(e.getMessage());
        }

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    @Test
    public void testValidInput() throws Exception {
        String[] args = {"../"};

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        if (StorageLayer.getStorage(process.getProcess()).getType() != STORAGE_TYPE.SQL) {
            return;
        }

        String recipeUserId = null;
        { // SignUp
            JsonObject optionsResponse = null;
            {
                JsonObject req = new JsonObject();
                req.addProperty("email", "test@example.com");
                req.addProperty("relyingPartyName", "Example");
                req.addProperty("relyingPartyId", "example.com");
                req.addProperty("origin", "http://example.com");

                optionsResponse = HttpRequestForTesting.sendJsonPOSTRequest(process.getProcess(), "",
                        "http://localhost:3567/recipe/webauthn/options/register", req, 1000, 1000, null,
                        SemVer.v5_3.get(), "webauthn");
            }

            JsonObject req = new JsonObject();
            req.addProperty("webauthnGeneratedOptionsId", optionsResponse.get("webauthnGeneratedOptionsId").getAsString());
            req.add("credential", generateCredential(optionsResponse));

            try {
                JsonObject resp = HttpRequestForTesting.sendJsonPOSTRequest(process.getProcess(), "",
                        "http://localhost:3567/recipe/webauthn/signup", req, 1000, 1000, null,
                        SemVer.v5_3.get(), "webauthn");
                recipeUserId = resp.get("recipeUserId").getAsString();
            } catch (HttpResponseException e) {
                fail(e.getMessage());
            }
        }
        { // Register
            JsonObject optionsResponse = null;
            {
                JsonObject req = new JsonObject();
                req.addProperty("email", "test@example.com");
                req.addProperty("relyingPartyName", "Example");
                req.addProperty("relyingPartyId", "example.com");
                req.addProperty("origin", "http://example.com");

                optionsResponse = HttpRequestForTesting.sendJsonPOSTRequest(process.getProcess(), "",
                        "http://localhost:3567/recipe/webauthn/options/register", req, 1000, 1000, null,
                        SemVer.v5_3.get(), "webauthn");
            }

            JsonObject req = new JsonObject();
            req.addProperty("recipeUserId", recipeUserId);
            req.addProperty("webauthnGeneratedOptionsId", optionsResponse.get("webauthnGeneratedOptionsId").getAsString());
            req.add("credential", generateCredential(optionsResponse));

            try {
                JsonObject resp = HttpRequestForTesting.sendJsonPOSTRequest(process.getProcess(), "",
                        "http://localhost:3567/recipe/webauthn/user/credential/register", req, 1000, 1000, null,
                        SemVer.v5_3.get(), "webauthn");
            } catch (HttpResponseException e) {
                fail(e.getMessage());
            }
        }

        Map<String, String> params = new HashMap<>();
        params.put("recipeUserId", recipeUserId);

        try {
            JsonObject resp = HttpRequestForTesting.sendGETRequest(process.getProcess(), "",
                    "http://localhost:3567/recipe/webauthn/user/credential/list", params, 1000, 1000, null,
                    SemVer.v5_3.get(), "webauthn");
            assertEquals("OK", resp.get("status").getAsString());
            assertEquals(2, resp.get("credentials").getAsJsonArray().size());
            checkResponseStructure(resp);
        } catch (HttpResponseException e) {
            fail(e.getMessage());
        }

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    private JsonObject generateCredential(JsonObject registerOptionsResponse) throws Exception {
        ClientPlatform clientPlatform = EmulatorUtil.createClientPlatform(EmulatorUtil.FIDO_U2F_AUTHENTICATOR);

        Map<String, PublicKeyCredentialCreationOptions> options = io.supertokens.test.webauthn.Utils.createPublicKeyCreationOptions(
                registerOptionsResponse);
        PublicKeyCredential<AuthenticatorAttestationResponse, RegistrationExtensionClientOutput> credential = io.supertokens.test.webauthn.Utils.createPasskey(
                clientPlatform, options.values().stream().findFirst().get());

        String attestationObject = Base64UrlUtil.encodeToString(credential.getAuthenticatorResponse().getAttestationObject());
        String clientDataJson = Base64UrlUtil.encodeToString(credential.getAuthenticatorResponse().getClientDataJSON());
        String rawId = Base64UrlUtil.encodeToString(credential.getRawId());

        JsonObject credentialJson = new Gson().toJsonTree(credential).getAsJsonObject();

        credentialJson.getAsJsonObject("response").addProperty("attestationObject", attestationObject);
        credentialJson.getAsJsonObject("response").addProperty("clientDataJSON", clientDataJson);
        credentialJson.addProperty("type", credential.getType());
        credentialJson.getAsJsonObject("response").remove("transports");
        credentialJson.remove("clientExtensionResults");
        credentialJson.addProperty("rawId", rawId);

        return credentialJson;
    }

    private void checkResponseStructure(JsonObject resp) throws Exception {
        assertEquals("OK", resp.get("status").getAsString());

        assertEquals(2, resp.entrySet().size());
        JsonArray credentials = resp.get("credentials").getAsJsonArray();
        for (JsonElement credentialElem : credentials) {
            JsonObject credential = credentialElem.getAsJsonObject();
            assertEquals(4, credential.entrySet().size());
            assertTrue(credential.has("webauthnCredentialId"));
            assertTrue(credential.has("relyingPartyId"));
            assertTrue(credential.has("recipeUserId"));
            assertTrue(credential.has("createdAt"));
        }
    }
}