package test.supers;

public class TestSuperClasses {

	public void callThruClass(Child ch) {
		ch.a();
		ch.b();
		ch.c();
	}
}

class GrandParent {
	public void a() {
	}
}

class Parent extends GrandParent {
	public void b() {
	}
}

class Child extends Parent {
	public void c() {
	}
}
