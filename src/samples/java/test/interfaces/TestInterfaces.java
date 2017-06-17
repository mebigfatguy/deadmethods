package test.interfaces;

public class TestInterfaces {

	public void callThruInterface(C c) {
		c.a();
		c.b();
		c.c();
		((W) c).plain();
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
