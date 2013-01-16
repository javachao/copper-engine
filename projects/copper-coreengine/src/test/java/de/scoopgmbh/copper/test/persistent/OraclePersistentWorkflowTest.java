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
package de.scoopgmbh.copper.test.persistent;

import java.sql.ResultSet;

import javax.sql.DataSource;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import de.scoopgmbh.copper.EngineState;
import de.scoopgmbh.copper.ProcessingEngine;
import de.scoopgmbh.copper.db.utility.RetryingTransaction;
import de.scoopgmbh.copper.persistent.PersistentScottyEngine;
import de.scoopgmbh.copper.test.backchannel.BackChannelQueue;
import de.scoopgmbh.copper.test.backchannel.WorkflowResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;


public class OraclePersistentWorkflowTest extends BasePersistentWorkflowTest {

	private static final String DS_CONTEXT = "/datasources/datasource-oracle.xml";
	private static final Logger logger = LoggerFactory.getLogger(OraclePersistentWorkflowTest.class);

	private static boolean dbmsAvailable = false;

	static {
		if (Boolean.getBoolean(Constants.SKIP_EXTERNAL_DB_TESTS_KEY)) {
			dbmsAvailable = true;
		}
		else {
			final ConfigurableApplicationContext context = new OraclePersistentWorkflowTest().createContext(DS_CONTEXT);
			try {
				DataSource ds = context.getBean(DataSource.class);
				ds.setLoginTimeout(10);
				ds.getConnection();
				dbmsAvailable = true;
			}
			catch(Exception e) {
				logger.error("Oracle DBMS not available! Skipping Oracle unit tests.",e);
				e.printStackTrace();
			}
			finally {
				context.close();
			}
		}
	}	

	@Override
	protected boolean skipTests() {
		return Boolean.getBoolean(Constants.SKIP_EXTERNAL_DB_TESTS_KEY);
	}

	@Test
	public void testAsnychResponse() throws Exception {
		if (!dbmsAvailable) fail("DBMS not available");
		super.testAsnychResponse(DS_CONTEXT);
	}

	@Test
	public void testAsnychResponseLargeData() throws Exception {
		if (!dbmsAvailable) fail("DBMS not available");
		super.testAsnychResponseLargeData(DS_CONTEXT,65536);
	}

	@Test
	public void testWithConnection() throws Exception {
		if (!dbmsAvailable) fail("DBMS not available");
		super.testWithConnection(DS_CONTEXT);
	}

	@Test
	public void testWithConnectionBulkInsert() throws Exception {
		if (!dbmsAvailable) fail("DBMS not available");
		super.testWithConnectionBulkInsert(DS_CONTEXT);
	}

	@Test
	public void testTimeouts() throws Exception {
		if (!dbmsAvailable) fail("DBMS not available");
		super.testTimeouts(DS_CONTEXT);
	}

	@Test
	public void testMultipleEngines() throws Exception {
		assumeFalse(skipTests());
		
		if (!dbmsAvailable) fail("DBMS not available");

		logger.info("running testMultipleEngines");
		final int NUMB = 50;
		final ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"/CopperTxnPersistentWorkflowTest/multiengine-oracle-unittest-context.xml"});
		cleanDB(context.getBean(DataSource.class));
		final PersistentScottyEngine engineRed = context.getBean("persistent.engine.red",PersistentScottyEngine.class);
		final PersistentScottyEngine engineBlue = context.getBean("persistent.engine.blue",PersistentScottyEngine.class);
		final BackChannelQueue backChannelQueue = context.getBean(BackChannelQueue.class);
		engineRed.startup();
		engineBlue.startup();
		try {
			assertEquals(EngineState.STARTED,engineRed.getEngineState());
			assertEquals(EngineState.STARTED,engineBlue.getEngineState());

			for (int i=0; i<NUMB; i++) {
				ProcessingEngine engine = i % 2 == 0 ? engineRed : engineBlue;
				engine.run(PersistentUnitTestWorkflow_NAME,null);
			}

			int x=0;
			long startTS = System.currentTimeMillis();
			while (x < NUMB && startTS+60000 > System.currentTimeMillis()) {
				WorkflowResult wfr = backChannelQueue.poll();
				if (wfr != null) {
					assertNull(wfr.getResult());
					assertNull(wfr.getException());
					x++;
				}
				else {
					Thread.sleep(50);
				}
			}
			if (x != NUMB) {
				fail("Test failed - Timeout - "+x+" responses so far");
			}
			Thread.sleep(1000);

			// check for late queue entries
			assertNull(backChannelQueue.poll());

			// check AuditTrail Log
			new RetryingTransaction(context.getBean(DataSource.class)) {
				@Override
				protected void execute() throws Exception {
					ResultSet rs = getConnection().createStatement().executeQuery("SELECT count(*) FROM COP_AUDIT_TRAIL_EVENT");
					rs.next();
					int count = rs.getInt(1);
					assertEquals(NUMB*6, count);
				}
			}.run();
		}
		finally {
			context.close();
		}
		assertEquals(EngineState.STOPPED,engineRed.getEngineState());
		assertEquals(EngineState.STOPPED,engineBlue.getEngineState());
		assertEquals(0,engineRed.getNumberOfWorkflowInstances());
		assertEquals(0,engineBlue.getNumberOfWorkflowInstances());

	}

	@Test
	public void testErrorHandlingInCoreEngine() throws Exception {
		if (!dbmsAvailable) fail("DBMS not available");
		super.testErrorHandlingInCoreEngine(DS_CONTEXT);
	}

	@Test
	public void testParentChildWorkflow() throws Exception {
		if (!dbmsAvailable) fail("DBMS not available");
		super.testParentChildWorkflow(DS_CONTEXT);
	}

	@Test
	public void testErrorKeepWorkflowInstanceInDB() throws Exception {
		if (!dbmsAvailable) fail("DBMS not available");
		super.testErrorKeepWorkflowInstanceInDB(DS_CONTEXT);
	}

	@Test
	public void testErrorHandlingInCoreEngine_restartAll() throws Exception {
		if (!dbmsAvailable) fail("DBMS not available");
		super.testErrorHandlingInCoreEngine_restartAll(DS_CONTEXT);
	}

	@Test
	public void testCompressedAuditTrail() throws Exception {
		if (!dbmsAvailable) fail("DBMS not available");
		super.testCompressedAuditTrail(DS_CONTEXT);
	}

	@Test
	public void testAutoCommit() throws Exception {
		if (!dbmsAvailable) fail("DBMS not available");
		super.testAutoCommit(DS_CONTEXT);
	}

	@Test
	public void testAuditTrailUncompressed() throws Exception {
		if (!dbmsAvailable) fail("DBMS not available");
		super.testAuditTrailUncompressed(DS_CONTEXT);
	}

	@Test
	public void testErrorHandlingWithWaitHook() throws Exception {
		if (!dbmsAvailable) fail("DBMS not available");
		super.testErrorHandlingWithWaitHook(DS_CONTEXT);
	}

	@Test
	public void testAuditTrailCustomSeqNr() throws Exception {
		if (!dbmsAvailable) fail("DBMS not available");
		super.testAuditTrailCustomSeqNr(DS_CONTEXT);
	}

	@Test
	public void testNotifyWithoutEarlyResponseHandling() throws Exception {
		if (!dbmsAvailable) fail("DBMS not available");
		super.testNotifyWithoutEarlyResponseHandling(DS_CONTEXT);
	}

}
