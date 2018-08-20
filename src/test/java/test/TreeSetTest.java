package test;

import java.util.Set;
import java.util.TreeSet;

public class TreeSetTest {
	public static void main(String args[]){
		Set<String> set = new TreeSet<>();
		System.out.println(set.add("123"));//true
		System.out.println(set.add("123"));//false
		System.out.println(set.add(null));//报错
		//System.out.println(set.remove(null));报错
		System.out.println(set.remove("123"));//true
		System.out.println(set.remove("123"));//false
	}
}
