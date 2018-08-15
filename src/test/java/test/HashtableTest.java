package test;

import java.util.Hashtable;
import java.util.Map;

public class HashtableTest {
	public static void main(String args[]){
		Map<String, String> map = new Hashtable<>();
		//System.out.println(map.put(null, "123"));//null
		System.out.println(map.put("456", null));//null
	}
}
