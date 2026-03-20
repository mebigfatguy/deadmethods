package com.mebigfatguy.deadmethods;

import java.io.IOException;

public abstract class AbstractProgressLogger implements ProgressLogger {

	private static final ThreadLocal<Boolean> DISABLED = new ThreadLocal<>();
	
	public boolean isDisabled() {
		return DISABLED.get();
	}
	
	@Override
	public <T> T disableWith(LogSupplier<T> producer) throws IOException {
		DISABLED.set(Boolean.TRUE);
		try {
			return producer.get();
		} finally {
			DISABLED.set(Boolean.FALSE);
		}
	}
}
