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
	
	public String toString(){
		return "<" + String.valueOf(t) + ", " + String.valueOf(v) + ">";
	}
	
	public int hashCode(){
		return (t == null ? 0 : t.hashCode()) * 47 + (v == null ? 0 : v.hashCode()) * 91;
	}
	
	@SuppressWarnings("unchecked")
	public boolean equals(Object o){
		return o instanceof Tuple && equals((Tuple<Object,Object>)o);
	}
	
	public boolean equals(Tuple<Object, Object> o){
		if(t == null){
			if(o.t != null) return false;
			if(v == null) return o.v == null;
		}
		if(!t.equals(o.t)) return false;
		if(v == null) return o.v == null;
		return v.equals(o.v);
	}

}
