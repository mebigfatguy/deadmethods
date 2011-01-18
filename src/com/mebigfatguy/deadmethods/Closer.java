package com.mebigfatguy.deadmethods;

import java.io.Closeable;

public class Closer {
	public static void close(Closeable c) {
		try {
			if (c != null)
				c.close();
		} catch (Exception e) {
		}
	}
}
