/*
 * Copyright (c) 2014 Evolveum
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

package com.evolveum.polygon.notes;

import com.evolveum.polygon.notes.util.DominoUtils;
import com.evolveum.polygon.notes.util.GuardedStringAccessor;
import lotus.domino.*;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;

public class DominoConnection {

    private static final Log LOG = Log.getLog(DominoConnection.class);

    private DominoConfiguration config;

    private Session session;
    private Database userDatabase;
    private AdministrationProcess administrationProcess;

    public DominoConnection(DominoConfiguration config) {
        this.config = config;
    }

    public Session getSession() {
        if (session == null) {
            LOG.ok("Opening session.");
            try {
                String ior = NotesFactory.getIOR(config.getIorHost());
                String password = GuardedStringAccessor.getString(config.getAdminPassword());
                session = NotesFactory.createSessionWithIOR(ior, config.getAdminName(), password);
            } catch (NotesException ex) {
                throw new ConnectorIOException("Couldn't open session through IOR on '" + config.getIorHost()
                        + "', reason: " + DominoUtils.getExceptionMessage(ex), ex);
            }
        }

        return session;
    }

    public void checkAlive() {
        if (!getSession().isValid()) {
            throw new ConnectorIOException("Domino session is not valid.");
        }
    }

    public Database getUserDatabase() {
        if (userDatabase != null) {
            return userDatabase;
        }

        Session session = getSession();
        String userDB = config.getUserDatabaseName();
        String registrationServer = config.getRegistrationServer();
        try {
            LOG.ok("Opening user database {0} on registration server {1}.", userDB, registrationServer);
            userDatabase = session.getDatabase(registrationServer, userDB, false);
        } catch (NotesException ex) {
            throw new ConnectorIOException("Couldn't open database '" + userDB + "' on server '" + registrationServer
                    + "', reason: " + DominoUtils.getExceptionMessage(ex), ex);
        }

        if (userDatabase == null) {
            throw new ConnectorIOException("Couldn't open database '" + userDB + "' on server '"
                    + registrationServer + "'.");
        }

        return userDatabase;
    }

    public AdministrationProcess getAdministrationProcess() {
        Session session = getSession();
        try {
            administrationProcess = session.createAdministrationProcess(config.getAdministrationServer());
            administrationProcess.setCertifierFile(config.getCertifierIdFile());
            administrationProcess.setCertifierPassword(DominoUtils.decode(config.getCertifierPassword()));
        } catch (NotesException ex) {
            DominoUtils.handleException(ex, "Couldn't create administration process", LOG);
        }

        return administrationProcess;
    }

    public void dispose() {
        if (session == null) {
            return;
        }

        try {
            session.recycle();
            session = null;
        } catch (NotesException ex) {
            throw new ConnectorIOException("Couldn't recycle notes session, reason: "
                    + DominoUtils.getExceptionMessage(ex), ex);
        }
    }
}
