package org.armanious;


public final class Tuple<T, V> {
	
	private final T t;
	private final V v;
	
	public Tuple(T t, V v){
		this.t = t;
		this.v = v;
	}
	
	public final T val1(){
		return t;
	}
	
	public final V val2(){
		return v;
	}

}
