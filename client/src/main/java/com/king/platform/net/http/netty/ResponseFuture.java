// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty;

import com.king.platform.net.http.HttpResponse;
import com.king.platform.net.http.netty.eventbus.*;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

public class ResponseFuture<T> extends CompletableFuture<HttpResponse<T>> {
	private final RequestEventBus requestEventBus;
	private final HttpRequestContext requestContext;

	public ResponseFuture(RequestEventBus requestEventBus, HttpRequestContext requestContext) {
		this.requestEventBus = requestEventBus;
		this.requestContext = requestContext;

		requestEventBus.subscribe(Event.ERROR, new RunOnceCallback2<HttpRequestContext, Throwable>() {
			@Override
			public void onFirstEvent(Event2 event, HttpRequestContext requestContext, Throwable throwable) {
				ResponseFuture.this.completeExceptionally(throwable);
			}
		});

		requestEventBus.subscribe(Event.onHttpResponseDone, new RunOnceCallback1<HttpResponse>() {
			@Override
			public void onFirstEvent(Event1 event, HttpResponse payload) {
				ResponseFuture.this.complete(payload);
			}
		});
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (isCancelled() || isDone() || isCompletedExceptionally()) {
			return false;
		}

		requestEventBus.triggerEvent(Event.ERROR, requestContext, new CancellationException());

		super.cancel(mayInterruptIfRunning);
		return true;
	}



	public static <T> CompletableFuture<HttpResponse<T>> error(Throwable error) {
		ResponseFuture<T> future = new ResponseFuture<>(new NoopRequestEventBus(), null);
		future.completeExceptionally(error);
		return future;
	}
}
