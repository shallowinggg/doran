/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shallowinggg.doran.transport.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for background thread
 */
public abstract class ServiceThread implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemotingHelper.DORAN_REMOTING);

    private static final long JOIN_TIME = 90 * 1000;
    protected final Thread thread;
    protected volatile boolean hasNotified = false;
    protected volatile boolean stopped = false;

    public ServiceThread() {
        this.thread = new Thread(this, this.getServiceName());
    }

    /**
     * Service name, used for thread name.
     *
     * @return service name
     */
    public abstract String getServiceName();

    public void start() {
        this.thread.start();
    }

    public void shutdown() {
        this.shutdown(false);
    }

    public void shutdown(final boolean interrupt) {
        this.stopped = true;
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("shutdown thread {}, interrupt: {}", this.getServiceName(), interrupt);
        }
        synchronized (this) {
            if (!this.hasNotified) {
                this.hasNotified = true;
                this.notify();
            }
        }

        try {
            if (interrupt) {
                this.thread.interrupt();
            }

            long beginTime = System.currentTimeMillis();
            this.thread.join(this.getJoinTime());
            long elapsedTime = System.currentTimeMillis() - beginTime;
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("join thread {} elapsed time(ms) {} {}", this.getServiceName(), elapsedTime, this.getJoinTime());
            }
        } catch (InterruptedException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Interrupted", e);
            }
        }
    }

    public long getJoinTime() {
        return JOIN_TIME;
    }

    public boolean isStopped() {
        return stopped;
    }
}
