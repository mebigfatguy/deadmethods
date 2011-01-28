package test.interfaces;

public class TestInterfaces {

	public void callThruInterface(W w) {
		w.a();
		w.b();
		w.c();
		w.plain();
	}

	static class W implements C {

		@Override
		public void a() {
		}

		@Override
		public void b() {
		}

		@Override
		public void c() {
		}

		public void plain() {
		}
	}
}
