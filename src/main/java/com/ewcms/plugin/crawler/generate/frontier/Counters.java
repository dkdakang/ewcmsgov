/**
 * Copyright (c)2010-2011 Enterprise Website Content Management System(EWCMS), All rights reserved.
 * EWCMS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * http://www.ewcms.com
 */
package com.ewcms.plugin.crawler.generate.frontier;

import com.ewcms.plugin.crawler.generate.crawler.Configurable;
import com.ewcms.plugin.crawler.generate.crawler.CrawlConfig;
import com.ewcms.plugin.crawler.generate.util.Util;
import com.sleepycat.je.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yasser Ganjisaffar <lastname at gmail dot com>
 */

public class Counters extends Configurable {
	
	public class ReservedCounterNames {
		public final static String SCHEDULED_PAGES = "Scheduled-Pages";
		public final static String PROCESSED_PAGES = "Processed-Pages";
	}

    protected Database statisticsDB = null;
	protected Environment env;

	protected final Object mutex = new Object();

	protected Map<String, Long> counterValues;

	public Counters(Environment env, CrawlConfig config) throws DatabaseException {
		super(config);

		this.env = env;
		this.counterValues = new HashMap<String, Long>();

		/*
		 * When crawling is set to be resumable, we have to keep the statistics
		 * in a transactional database to make sure they are not lost if crawler
		 * is crashed or terminated unexpectedly.
		 */
		if (config.isResumableCrawling()) {
			DatabaseConfig dbConfig = new DatabaseConfig();
			dbConfig.setAllowCreate(true);
			dbConfig.setTransactional(true);
			dbConfig.setDeferredWrite(false);
			statisticsDB = env.openDatabase(null, "Statistics", dbConfig);
			
			OperationStatus result;
			DatabaseEntry key = new DatabaseEntry();
			DatabaseEntry value = new DatabaseEntry();
			Transaction tnx = env.beginTransaction(null, null);
			Cursor cursor = statisticsDB.openCursor(tnx, null);
			result = cursor.getFirst(key, value, null);

			while (result == OperationStatus.SUCCESS) {
				if (value.getData().length > 0) {
					String name = new String(key.getData());
					long counterValue = Util.byteArray2Long(value.getData());
					counterValues.put(name, counterValue);
				}
				result = cursor.getNext(key, value, null);
			}
			cursor.close();
			tnx.commit();
		}
	}

	public long getValue(String name) {
		synchronized (mutex) {
			Long value = counterValues.get(name);
			if (value == null) {
				return 0;
			}
			return value;
		}
	}

	public void setValue(String name, long value) {
		synchronized (mutex) {
			try {
				counterValues.put(name, value);
				if (statisticsDB != null) {
					Transaction txn = env.beginTransaction(null, null);					
					statisticsDB.put(txn, new DatabaseEntry(name.getBytes()),
							new DatabaseEntry(Util.long2ByteArray(value)));
					txn.commit();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void increment(String name) {
		increment(name, 1);
	}

	public void increment(String name, long addition) {
		synchronized (mutex) {
			long prevValue = getValue(name);
			setValue(name, prevValue + addition);
		}
	}

	public void sync() {
		if (config.isResumableCrawling()) {
			return;
		}
		if (statisticsDB == null) {
			return;
		}
		try {
			statisticsDB.sync();
		} catch (DatabaseException e) {
			e.printStackTrace();
		}
	}

	public void close() {
		try {
			if (statisticsDB != null) {
				statisticsDB.close();
			}
		} catch (DatabaseException e) {
			e.printStackTrace();
		}
	}
}
