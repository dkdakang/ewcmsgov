/**
 * Copyright (c)2010-2011 Enterprise Website Content Management System(EWCMS), All rights reserved.
 * EWCMS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * http://www.ewcms.com
 */
package com.ewcms.plugin.crawler.generate.crawler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ewcms.plugin.crawler.generate.fetcher.PageFetchStatus;
import com.ewcms.plugin.crawler.generate.fetcher.PageFetcher;
import com.ewcms.plugin.crawler.generate.frontier.DocIDServer;
import com.ewcms.plugin.crawler.generate.frontier.Frontier;
import com.ewcms.plugin.crawler.generate.parser.HtmlParseData;
import com.ewcms.plugin.crawler.generate.parser.ParseData;
import com.ewcms.plugin.crawler.generate.parser.Parser;
import com.ewcms.plugin.crawler.generate.robotstxt.RobotstxtServer;
import com.ewcms.plugin.crawler.generate.url.WebURL;

/**
 * WebCrawler class in the Runnable class that is executed by each crawler
 * thread.
 * 
 * @author Yasser Ganjisaffar <lastname at gmail dot com>
 */
public class WebCrawler implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(WebCrawler.class.getName());

	/**
	 * The id associated to the crawler thread running this instance
	 */
	protected int myId;

	/**
	 * The controller instance that has created this crawler thread. This
	 * reference to the controller can be used for getting configurations of the
	 * current crawl or adding new seeds during runtime.
	 */
	protected CrawlController myController;

	/**
	 * The thread within which this crawler instance is running.
	 */
	private Thread myThread;

	/**
	 * The parser that is used by this crawler instance to parse the content of
	 * the fetched pages.
	 */
	private Parser parser;

	/**
	 * The fetcher that is used by this crawler instance to fetch the content of
	 * pages from the web.
	 */
	private PageFetcher pageFetcher;

	/**
	 * The RobotstxtServer instance that is used by this crawler instance to
	 * determine whether the crawler is allowed to crawl the content of each
	 * page.
	 */
	private RobotstxtServer robotstxtServer;

	/**
	 * The DocIDServer that is used by this crawler instance to map each URL to
	 * a unique docid.
	 */
	private DocIDServer docIdServer;

	/**
	 * The Frontier object that manages the crawl queue.
	 */
	private Frontier frontier;

	/**
	 * Is the current crawler instance waiting for new URLs? This field is
	 * mainly used by the controller to detect whether all of the crawler
	 * instances are waiting for new URLs and therefore there is no more work
	 * and crawling can be stopped.
	 */
	private boolean isWaitingForNewURLs;
	
	private Map<String,Object> passingParameters;

	/**
	 * Initializes the current instance of the crawler
	 * 
	 * @param myId
	 *            the id of this crawler instance
	 * @param crawlController
	 *            the controller that manages this crawling session
	 */
	public void init(int myId, CrawlController crawlController) {
		this.myId = myId;
		this.pageFetcher = crawlController.getPageFetcher();
		this.robotstxtServer = crawlController.getRobotstxtServer();
		this.docIdServer = crawlController.getDocIdServer();
		this.frontier = crawlController.getFrontier();
		this.parser = new Parser(crawlController.getConfig());
		this.myController = crawlController;
		this.isWaitingForNewURLs = false;
		this.passingParameters = crawlController.getPassingParameters();
	}

	/**
	 * Get the id of the current crawler instance
	 * 
	 * @return the id of the current crawler instance
	 */
	public int getMyId() {
		return myId;
	}

	public CrawlController getMyController() {
		return myController;
	}

	/**
	 * This function is called just before starting the crawl by this crawler
	 * instance. It can be used for setting up the data structures or
	 * initializations needed by this crawler instance.
	 */
	public void onStart() {
	}

	/**
	 * This function is called just before the termination of the current
	 * crawler instance. It can be used for persisting in-memory data or other
	 * finalization tasks.
	 */
	public void onBeforeExit() {
	}

	/**
	 * The CrawlController instance that has created this crawler instance will
	 * call this function just before terminating this crawler thread. Classes
	 * that extend WebCrawler can override this function to pass their local
	 * data to their controller. The controller then puts these local data in a
	 * List that can then be used for processing the local data of crawlers (if
	 * needed).
	 */
	public Object getMyLocalData() {
		return null;
	}

	public void run() {
		onStart();
		while (true) {
			List<WebURL> assignedURLs = new ArrayList<WebURL>(50);
			isWaitingForNewURLs = true;
			frontier.getNextURLs(50, assignedURLs);
			isWaitingForNewURLs = false;
			if (assignedURLs.size() == 0) {
				if (frontier.isFinished()) {
					return;
				}
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				for (WebURL curURL : assignedURLs) {
					if (curURL != null) {
						processPage(curURL);
						frontier.setProcessed(curURL);
					}
					if (myController.isShuttingDown()) {
						logger.info("Exiting because of controller shutdown.");
						return;
					}
				}
			}
		}
	}

	/**
	 * Classes that extends WebCrawler can overwrite this function to tell the
	 * crawler whether the given url should be crawled or not. The following
	 * implementation indicates that all urls should be included in the crawl.
	 * 
	 * @param url
	 *            the url which we are interested to know whether it should be
	 *            included in the crawl or not.
	 * @return if the url should be included in the crawl it returns true,
	 *         otherwise false is returned.
	 */
	public boolean shouldVisit(WebURL url) {
		return true;
	}

	/**
	 * Classes that extends WebCrawler can overwrite this function to process
	 * the content of the fetched and parsed page.
	 * 
	 * @param page
	 *            the page object that is just fetched and parsed.
	 */
	public void visit(Page page) {
	}

	private int processPage(WebURL curURL) {
		if (curURL == null) {
			return -1;
		}
		try {
			int statusCode = pageFetcher.fetchHeader(curURL);
			if (statusCode != PageFetchStatus.OK) {
				if (statusCode == PageFetchStatus.Moved) {
					if (myController.getConfig().isFollowRedirects()) {
						String movedToUrl = curURL.getURL();
						if (movedToUrl == null) {
							return PageFetchStatus.MovedToUnknownLocation;
						}
						int newDocId = docIdServer.getDocId(movedToUrl);
						if (newDocId > 0) {
							return PageFetchStatus.RedirectedPageIsSeen;
						} else {
							WebURL webURL = new WebURL();
							webURL.setURL(movedToUrl);
							webURL.setParentDocid(curURL.getParentDocid());
							webURL.setDepth(curURL.getDepth());
							webURL.setDocid(-1);
							if (shouldVisit(webURL) && robotstxtServer.allows(webURL)) {
								webURL.setDocid(docIdServer.getNewDocID(movedToUrl));
								frontier.schedule(webURL);
							}
						}
					}
					return PageFetchStatus.Moved;
				} else if (statusCode == PageFetchStatus.PageTooBig) {
					logger.info("Skipping a page which was bigger than max allowed size: " + curURL.getURL());
				}
				return statusCode;
			}

			if (!curURL.getURL().equals(pageFetcher.getFetchedUrl())) {
				if (docIdServer.isSeenBefore(pageFetcher.getFetchedUrl())) {
					return PageFetchStatus.RedirectedPageIsSeen;
				}
				curURL.setURL(pageFetcher.getFetchedUrl());
				curURL.setDocid(docIdServer.getNewDocID(pageFetcher.getFetchedUrl()));
			}

			Page page = new Page(curURL);
			int docid = curURL.getDocid();
			if (pageFetcher.fetchContent(page) && parser.parse(page, curURL.getURL())) {
				ParseData parseData = page.getParseData();
				if (parseData instanceof HtmlParseData) {
					HtmlParseData htmlParseData = (HtmlParseData) parseData;

					List<WebURL> toSchedule = new ArrayList<WebURL>();
					int maxCrawlDepth = myController.getConfig().getMaxDepthOfCrawling();
					for (WebURL webURL : htmlParseData.getOutgoingUrls()) {
						webURL.setParentDocid(docid);
						int newdocid = docIdServer.getDocId(webURL.getURL());
						if (newdocid > 0) {
							// This is not the first time that this Url is
							// visited. So, we set the depth to a negative
							// number.
							webURL.setDepth((short) -1);
							webURL.setDocid(newdocid);
						} else {
							webURL.setDocid(-1);
							webURL.setDepth((short) (curURL.getDepth() + 1));
							if (maxCrawlDepth == -1 || curURL.getDepth() < maxCrawlDepth) {
								if (shouldVisit(webURL) && robotstxtServer.allows(webURL)) {
									webURL.setDocid(docIdServer.getNewDocID(webURL.getURL()));
									toSchedule.add(webURL);
								}
							}
						}
					}
					frontier.scheduleAll(toSchedule);
				}
				visit(page);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage() + ", while processing: " + curURL.getURL());
		} finally {
			pageFetcher.discardContentIfNotConsumed();
		}
		return 0;
	}

	public Thread getThread() {
		return myThread;
	}

	public void setThread(Thread myThread) {
		this.myThread = myThread;
	}

	public boolean isNotWaitingForNewURLs() {
		return !isWaitingForNewURLs;
	}

	public Map<String, Object> getPassingParameters() {
		return passingParameters;
	}

	public void setPassingParameters(Map<String, Object> passingParameters) {
		this.passingParameters = passingParameters;
	}
}
