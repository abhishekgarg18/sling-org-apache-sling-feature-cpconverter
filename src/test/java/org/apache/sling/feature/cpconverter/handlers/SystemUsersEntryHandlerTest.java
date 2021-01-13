/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.feature.cpconverter.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.List;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.cpconverter.accesscontrol.DefaultAclManager;
import org.apache.sling.repoinit.parser.RepoInitParser;
import org.apache.sling.repoinit.parser.impl.RepoInitParserService;
import org.apache.sling.repoinit.parser.operations.Operation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SystemUsersEntryHandlerTest {

    private SystemUsersEntryHandler systemUsersEntryHandler;

    @Before
    public void setUp() {
        systemUsersEntryHandler = new SystemUsersEntryHandler();
    }

    @After
    public void tearDown() {
        systemUsersEntryHandler = null;
    }

    @Test
    public void doesNotMatch() {
        assertFalse(systemUsersEntryHandler.matches("/this/is/a/path/not/pointing/to/a/valid/configuration.asd"));
        assertFalse(systemUsersEntryHandler.matches("/home/users/system/asd-share-commons/asd-index-definition-reader/.content.xml"));
    }

    @Test
    public void matches() {
        assertTrue(systemUsersEntryHandler.matches("/jcr_root/home/users/system/asd-share-commons/asd-index-definition-reader/.content.xml"));
    }

    @Test
    public void parseSystemUser() throws Exception {
        String path = "/jcr_root/home/users/system/asd-share-commons/asd-index-definition-reader/.content.xml";
        Extension repoinitExtension = parseAndSetRepoinit(path);

        assertNotNull(repoinitExtension);
        assertEquals(ExtensionType.TEXT, repoinitExtension.getType());
        assertTrue(repoinitExtension.isRequired());

        String expected = "create path (rep:AuthorizableFolder) /home/users/system/asd-share-commons" + System.lineSeparator() + // SLING-8586
                "create service user asd-share-commons-asd-index-definition-reader-service with path /home/users/system/asd-share-commons" + System.lineSeparator();
        String actual = repoinitExtension.getText();
        assertEquals(expected, actual);

        RepoInitParser repoInitParser = new RepoInitParserService();
        List<Operation> operations = repoInitParser.parse(new StringReader(actual));
        assertFalse(operations.isEmpty());
    }

    @Test
    public void unrecognisedSystemUserJcrNode() throws Exception {
        String path = "/jcr_root/home/users/system/asd-share-commons/asd-index-definition-invalid/.content.xml";
        Extension repoinitExtension = parseAndSetRepoinit(path);
        assertNull(repoinitExtension);
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/SLING-9970">SLING-9970</a>
     */
    @Test
    public void testSystemUserPathIsConvertedToRepositoryPath() throws Exception {
        String path = "/jcr_root/home/users/system/_my_feature/_my_user-node/.content.xml";
        Extension repoinitExtension = parseAndSetRepoinit(path);
        assertNotNull(repoinitExtension);

        String actual = repoinitExtension.getText();
        assertFalse(actual.contains("/jcr_root/home/users/system/_my_feature"));
        assertFalse(actual.contains("/home/users/system/_my_feature"));
        assertTrue(actual.contains("/home/users/system/my:feature"));
    }

    private Extension parseAndSetRepoinit(String path) throws Exception {
        return TestUtils.createRepoInitExtension(systemUsersEntryHandler, new DefaultAclManager(), path, getClass().getResourceAsStream(path.substring(1)));
    }
}
