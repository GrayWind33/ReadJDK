package test;

import java.util.TreeMap;

public class TreeMapTest {
	public static void main(String args[]){
		TreeMap<String, String> tmap = new TreeMap<>();
		System.out.println(tmap.put("123", null));//null
		//tmap.put(null, "123");报错
		System.out.println(tmap.get("123"));//null
		tmap.descendingKeySet();
	}
	
}
