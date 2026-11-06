package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.parser.StreamParser;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class SelectorIT {

    @Test
    public void multiThreadHas() throws InterruptedException {
        final String html = "<div id=1></div><div id=2><p>One</p><p>Two</p>";
        final Evaluator eval = QueryParser.parse("div:has(p)");

        int numThreads = 20;
        int numThreadLoops = 5;

        SelectorIT.ThreadCatcher catcher = new SelectorIT.ThreadCatcher();

        AtomicInteger nextRunner = new AtomicInteger();
        ThreadFactory runnerThreads = runnable -> {
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setName("Runner-" + nextRunner.getAndIncrement());
            thread.setUncaughtExceptionHandler(catcher);
            return thread;
        };
        ExecutorService executor = Executors.newFixedThreadPool(numThreads, runnerThreads);
        for (int threadNum = 0; threadNum < numThreads; threadNum++) {
            executor.execute(() -> {
                Document doc = Jsoup.parse(html);
                for (int loop = 0; loop < numThreadLoops; loop++) {
                    Elements els = doc.select(eval);
                    assertEquals(1, els.size());
                    assertEquals("2", els.get(0).id());
                }
            });
        }

        // now wait for them all
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.MINUTES));

        assertEquals(0, catcher.exceptionCount.get());
    }

    static class ThreadCatcher implements Thread.UncaughtExceptionHandler {
        AtomicInteger exceptionCount = new AtomicInteger();

        @Override
        public void uncaughtException(Thread t, Throwable e) {

            e.printStackTrace();
            exceptionCount.incrementAndGet();
        }
    }
}
