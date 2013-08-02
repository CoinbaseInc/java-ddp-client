/*
* (c)Copyright 2013 Ken Yee, KEY Enterprise Solutions 
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

package com.keysolutions.ddpclient.test;

import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.keysolutions.ddpclient.DDPClient;
import com.keysolutions.ddpclient.EmailAuth;
import com.keysolutions.ddpclient.TokenAuth;
import com.keysolutions.ddpclient.UsernameAuth;
import com.keysolutions.ddpclient.test.DDPTestClientObserver.DDPSTATE;

import junit.framework.TestCase;

/**
 * Tests for authentication
 * @author kenyee
 */
public class TestDDPAuth extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    /**
     * Verifies that gson converts auth info classes properly to JSON
     * @throws Exception
     */
    public void testGson2JSonAuthInfo() throws Exception {
        // test username/password is encoded properly
        UsernameAuth userpass = new UsernameAuth("test", "pw");
        Gson gson = new Gson();
        String jsonUserpass = gson.toJson(userpass);
        assertEquals("{\"password\":\"pw\",\"user\":{\"username\":\"test\"}}", jsonUserpass);
        // test email/password is encoded properly
        EmailAuth emailpass = new EmailAuth("test@me.com", "pw");
        String jsonEmailpass = gson.toJson(emailpass);
        assertEquals("{\"password\":\"pw\",\"user\":{\"email\":\"test@me.com\"}}", jsonEmailpass);
        // test resumetoken is encoded properly
        TokenAuth token = new TokenAuth("mytoken");
        String jsonToken = gson.toJson(token);
        assertEquals("{\"resume\":\"mytoken\"}", jsonToken);       
    }
    
    /**
     * Verifies that errors are handled properly
     * @throws Exception
     */
    public void testHandleError() throws Exception {
        DDPClient ddp = new DDPClient("", 0);
        DDPTestClientObserver obs = new DDPTestClientObserver();
        ddp.addObserver(obs);
        // do this convoluted thing to test a private method
        Method method = DDPClient.class.getDeclaredMethod("handleError", Exception.class);
        method.setAccessible(true);
        method.invoke(ddp, new Exception("ignore exception"));
        assertEquals("WebSocketClient", obs.mErrorSource);
        assertEquals("ignore exception", obs.mErrorMsg);
    }
    
    /**
     * Verifies that a connection can be closed
     * @throws Exception
     */
    public void testConnectionClosed() throws Exception {
        DDPClient ddp = new DDPClient("", 0);
        DDPTestClientObserver obs = new DDPTestClientObserver();
        ddp.addObserver(obs);
        // do this convoluted thing to test a private method
        Method method = DDPClient.class.getDeclaredMethod("connectionClosed", int.class, String.class, boolean.class);
        method.setAccessible(true);
        method.invoke(ddp, 5, "test", true);
        assertEquals(5, obs.mCloseCode);
        assertEquals("test", obs.mCloseReason);
        assertEquals(true, obs.mCloseFromRemote);
    }
    
    /**
     * Verifies that a bad login is rejected
     * @throws Exception
     */
    public void testBadLogin() throws Exception {
        //// test out invalid login username/email
        // create DDP client instance and hook testobserver to it
        DDPClient ddp = new DDPClient(TestConstants.sMeteorIp, TestConstants.sMeteorPort);
        DDPTestClientObserver obs = new DDPTestClientObserver();
        ddp.addObserver(obs);                    
        // make connection to Meteor server
        ddp.connect();          

        // we need to wait a bit before the socket is opened but make sure it's successful
        Thread.sleep(500);
        assertTrue(obs.mDdpState == DDPSTATE.Connected);
        
        // [password: passwordstring,
        // user: {
        //    username: usernamestring
        // or 
        //    email: emailstring
        // or 
        //    resume: resumetoken (no password required)
        //  }]
        Object[] methodArgs = new Object[1];
        EmailAuth emailpass = new EmailAuth("invalid@invalid.com", "password");
        methodArgs[0] = emailpass;
        int methodId = ddp.call("login", methodArgs, obs);
        assertEquals(1, methodId);  // first ID should be 1
        Thread.sleep(500);
        assertTrue(obs.mDdpState == DDPSTATE.Connected);
        assertEquals(403, obs.mErrorCode);
        assertEquals("User not found", obs.mErrorReason);
        assertEquals("User not found [403]", obs.mErrorMsg);
        assertEquals("Meteor.Error", obs.mErrorType);
    }
    
    /**
     * Verifies that a bad password is rejected
     * @throws Exception
     */
    public void testBadPassword() throws Exception {
        //// test out invalid password
        // create DDP client instance and hook testobserver to it
        DDPClient ddp = new DDPClient(TestConstants.sMeteorIp, TestConstants.sMeteorPort);
        DDPTestClientObserver obs = new DDPTestClientObserver();
        ddp.addObserver(obs);                    
        // make connection to Meteor server
        ddp.connect();          

        // we need to wait a bit before the socket is opened but make sure it's successful
        Thread.sleep(500);
        assertTrue(obs.mDdpState == DDPSTATE.Connected);
        
        // [password: passwordstring,
        // user: {
        //    username: usernamestring
        // or 
        //    email: emailstring
        // or 
        //    resume: resumetoken (no password required)
        //  }]
        Object[] methodArgs = new Object[1];
        EmailAuth emailpass = new EmailAuth("invalid@invalid.com", "password");
        methodArgs[0] = emailpass;
        int methodId = ddp.call("login", methodArgs, obs);
        assertEquals(1, methodId);  // first ID should be 1
        Thread.sleep(500);
        assertTrue(obs.mDdpState == DDPSTATE.Connected);
        assertEquals(403, obs.mErrorCode);
        assertEquals("User not found", obs.mErrorReason);
        assertEquals("User not found [403]", obs.mErrorMsg);
        assertEquals("Meteor.Error", obs.mErrorType);
    }

    /**
     * Verifies that email/password login and resume tokens work
     * @throws Exception
     */
    public void testLogin() throws Exception {
        //TODO: does this belong inside the Java DDP client?
        //// test out regular login
        // create DDP client instance and hook testobserver to it
        DDPClient ddp = new DDPClient(TestConstants.sMeteorIp, TestConstants.sMeteorPort);
        DDPTestClientObserver obs = new DDPTestClientObserver();
        ddp.addObserver(obs);                    
        // make connection to Meteor server
        ddp.connect();          

        // we need to wait a bit before the socket is opened but make sure it's successful
        Thread.sleep(500);
        assertTrue(obs.mDdpState == DDPSTATE.Connected);
        
        // [password: passwordstring,
        // user: {
        //    username: usernamestring
        // or 
        //    email: emailstring
        // or 
        //    resume: resumetoken (no password required)
        //  }]
        Object[] methodArgs = new Object[1];
        EmailAuth emailpass = new EmailAuth(TestConstants.sMeteorUsername, TestConstants.sMeteorPassword);
        methodArgs[0] = emailpass;
        int methodId = ddp.call("login", methodArgs, obs);
        assertEquals(1, methodId);  // first ID should be 1
        
        // we should get a message back after a bit..make sure it's successful
        // we need to grab the "token" from the result for the next test
        Thread.sleep(500);
        assertTrue(obs.mDdpState == DDPSTATE.LoggedIn);
        
        // verify that we have the user in the users collection after login
        assertTrue(obs.mCollections.get("users").size() == 1);
        
        //// test out resume token
        String resumeToken = obs.mResumeToken;
        ddp = new DDPClient(TestConstants.sMeteorIp, TestConstants.sMeteorPort);
        obs = new DDPTestClientObserver();
        ddp.addObserver(obs);                    
        // make connection to Meteor server
        ddp.connect();          

        // we need to wait a bit before the socket is opened but make sure it's successful
        Thread.sleep(500);
        assertTrue(obs.mDdpState == DDPSTATE.Connected);
        
        TokenAuth token = new TokenAuth(resumeToken);
        methodArgs[0] = token;
        methodId = ddp.call("login", methodArgs, obs);
        assertEquals(1, methodId);  // first ID should be 1
        Thread.sleep(500);
        assertTrue(obs.mDdpState == DDPSTATE.LoggedIn);
        
        // verify that we have the user in the users collection after login
        assertTrue(obs.mCollections.get("users").size() == 1);
    }
    
    /**
     * Tests that we can create a user and log in
     * @throws URISyntaxException 
     * @throws InterruptedException 
     */
    @SuppressWarnings("unchecked")
    public void testCreateUser() throws URISyntaxException, InterruptedException {
        //TODO: does this belong inside the Java DDP client?
        // create DDP client instance and hook testobserver to it
        DDPClient ddp = new DDPClient(TestConstants.sMeteorIp, TestConstants.sMeteorPort);
        DDPTestClientObserver obs = new DDPTestClientObserver();
        ddp.addObserver(obs);                    
        // make connection to Meteor server
        ddp.connect();          

        // we need to wait a bit before the socket is opened but make sure it's successful
        Thread.sleep(500);
        assertTrue(obs.mDdpState == DDPSTATE.Connected);
        
        // subscribe to user collection
        ddp.subscribe("users", new Object[] {});
        
        // delete old user first in case this test has been run before
        Object[] methodArgs = new Object[1];
        methodArgs[0] = "test2@test.com";
        ddp.call("deleteUser", methodArgs, obs);
        
        // we need to wait a bit in case there was a deletion
        Thread.sleep(500);

        // make sure user doesn't exist
        Map<String, Object> userColl = obs.mCollections.get("users");
        assertNotNull(userColl);
        boolean foundUser = false;
        for (Entry<String, Object> entry : userColl.entrySet()) {
            Map<String, Object> fields = (Map<String, Object>) entry.getValue();
            ArrayList<Map<String,Object>> emails = (ArrayList<Map<String, Object>>) fields.get("emails");
            assertFalse(emails.get(0).get("address").equals("test2@test.com"));
        }
        
        // create new user
        Map<String,Object> options = new HashMap<String,Object>();
        methodArgs[0] = options;
        options.put("username", "test2@test.com");
        options.put("email", "test2@test.com");
        options.put("password", "1234");
        ddp.call("createUser", methodArgs);
        
        // we need to wait a bit for the insertion or error
        Thread.sleep(500);
        
        // make sure we have no errors
        assertEquals(0, obs.mErrorCode);
        
        // check that users collection has this user
        userColl = obs.mCollections.get("users");
        assertNotNull(userColl);
        foundUser = false;
        for (Entry<String, Object> entry : userColl.entrySet()) {
            Map<String, Object> fields = (Map<String, Object>) entry.getValue();
            ArrayList<Map<String,Object>> emails = (ArrayList<Map<String, Object>>) fields.get("emails");
            if (emails.get(0).get("address").equals("test2@test.com")) {
                foundUser = true;
                break;
            }
        }
        assertTrue(foundUser);
    }
    
    //TODO: test SRP login
    // \"msg\":\"method\",\"method\":\"beginPasswordExchange\",\"params\":[{\"A\":\"df5a724a7e8ecadb707bdeda605b153e9334aaa6390ffe981500583087120b296f92d98ed73abf0f374bf650db26ff3ca392422455cb878ce35868da6e94549d306448e377b41183d33908fb7b36d81e476cce4be7d7b3ea3a5f9a6c3a07fde1a3b0decf8ca4ae28d5bdf29006ef5926aac4cfb97040cbf8375b52c583610b74\",\"user\":{\"email\":\"test@test.com\"}}],\"id\":\"1\"}"]
    // a["{\"msg\":\"result\",\"id\":\"1\",\"result\":{\"identity\":\"SMMN6aCqEdADZHSk8\",\"salt\":\"4vyLDr8dix7YYsWXq\",\"B\":\"300fb1ce2b5e85a3fa7a850f6433a8490f1a0eb3dad8975ffd06063d0b85e8e25c7860cb940dfc5a8483de84c0459c202291f4d888b1ab27e55b051383c3b457d2729666d3f6a75bd5c3caabf770fda5554a49b108c934c1045921a5fc0a3eb95aa33e27d7aa7fe98140b74fa2cb5fc077c6382314c0e1f04408dff2fa56e7a2\"}}"]
    // ["{\"msg\":\"method\",\"method\":\"login\",\"params\":[{\"srp\":{\"M\":\"95ca0290f8ef20a2bccd0ed26c82e777fdedb3b90ec07cda880876e763fff525\"}}],\"id\":\"2\"}"]
    // a["{\"msg\":\"result\",\"id\":\"2\",\"result\":{\"token\":\"BdXLCetbZ3nMaF5nM\",\"id\":\"LQLc7rixstaMZBg8K\",\"HAMK\":\"082c35c1c9f9a2413be960bd5c0d8a76619ae1cb533895d3f5d87759f667d14f\"}}"]
    // http://stackoverflow.com/questions/16729992/authenticating-with-meteor-via-ddp-and-srp/17558300#17558300
}
