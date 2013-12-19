/*
 * Copyright 2002-2013 SCOOP Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.copperengine.core.db.utility.oracle.c3p0;

import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mchange.v2.c3p0.AbstractConnectionCustomizer;

public class OracleConnectionCustomizer extends AbstractConnectionCustomizer {

    private static final Logger logger = LoggerFactory.getLogger(OracleConnectionCustomizer.class);

    @Override
    public void onAcquire(Connection c, String parentDataSourceIdentityToken) throws Exception {
        logger.info("Customizing OracleConnection " + c);
        c.setAutoCommit(false);
        // OracleConnection oc = (OracleConnection)c;
        // oc.setImplicitCachingEnabled(true);
        // oc.setStatementCacheSize(20);
        // oc.setDefaultRowPrefetch(50);
    }
}
