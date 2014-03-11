/*
 * The MIT License
 *
 * Copyright 2012 Sony Mobile Communications AB. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.sonyericsson.jenkins.plugins.bfa.db;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import javax.naming.AuthenticationException;

import com.mongodb.DB;
import hudson.util.Secret;
import org.jvnet.hudson.test.HudsonTestCase;
import org.powermock.reflect.Whitebox;

//CS IGNORE MagicNumber FOR NEXT 100 LINES. REASON: TestData.

/**
 * Tests for Authentication with Mongo DB.
 *
 * @author Tomas Westling &lt;tomas.westling@sonymobile.com&gt;
 */
public class MongoDBAuthenticationHudsonTest extends HudsonTestCase {

    /**
     * Tests that we can authenticate towards Mongo DB.
     * @throws Exception if so.
     */
  public void testAuthenticate() throws Exception {
      KnowledgeBase kb = new MongoDBKnowledgeBase("", 27017, "mydb", "user", Secret.fromString("password"), false, false);
      DB db = mock(DB.class);
      when(db.authenticate("user", "password".toCharArray())).thenReturn(false);
      Whitebox.setInternalState(kb, db);
      try {
          kb.getShallowCauses();
          fail("No AuthenticationException thrown!");
          //CS IGNORE EmptyBlock FOR NEXT 2 LINES. REASON: this should be thrown.
      } catch (AuthenticationException e) {
      }
    }
}
