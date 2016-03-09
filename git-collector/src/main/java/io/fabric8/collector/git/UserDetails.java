/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.collector.git;

import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.CredentialsProvider;

import java.io.File;

/**
 */
public class UserDetails {
    private String remote = "origin";
    private File sshPrivateKey;
    private File sshPublicKey;

    public CredentialsProvider createCredentialsProvider() {
        // TODO
        return null;
    }

    public PersonIdent createPersonIdent() {
        // TODO
        return null;
    }

    public String getRemote() {
        return remote;
    }

    public void setRemote(String remote) {
        this.remote = remote;
    }

    public File getSshPrivateKey() {
        return sshPrivateKey;
    }

    public void setSshPrivateKey(File sshPrivateKey) {
        this.sshPrivateKey = sshPrivateKey;
    }

    public File getSshPublicKey() {
        return sshPublicKey;
    }

    public void setSshPublicKey(File sshPublicKey) {
        this.sshPublicKey = sshPublicKey;
    }
}
