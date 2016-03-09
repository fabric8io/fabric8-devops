/**
 * Copyright 2005-2015 Red Hat, Inc.
 * <p/>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.collector.git.elasticsearch;

import io.fabric8.collector.elasticsearch.DTOSupport;
import org.eclipse.jgit.lib.PersonIdent;

import java.util.Date;

/**
 * Represents information about an identity
 */
public class PersonIdentDTO extends DTOSupport {
    private final String name;
    private final String emailAddress;
    private final String timeZone;
    private final Date when;

    public PersonIdentDTO(PersonIdent ident) {
        this.name = ident.getName();
        this.emailAddress = ident.getEmailAddress();
        this.timeZone = ident.getTimeZone().getID();
        this.when = ident.getWhen();
    }

    public static PersonIdentDTO newInstance(PersonIdent ident) {
        if (ident == null) {
            return null;
        }
        return new PersonIdentDTO(ident);

    }

    @Override
    public String toString() {
        return "PersonIdentDTO{" +
                "name='" + name + '\'' +
                ", emailAddress='" + emailAddress + '\'' +
                '}';
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getName() {
        return name;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public Date getWhen() {
        return when;
    }
}
