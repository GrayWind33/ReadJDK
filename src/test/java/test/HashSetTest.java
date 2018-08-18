package test;

import java.util.HashSet;
import java.util.Set;

public class HashSetTest {
	public static void main(String args[]){
		Set<String> set = new HashSet<>();
		System.out.println(set.add("123"));//true
		System.out.println(set.add("123"));//false
		System.out.println(set.add(null));//true
		System.out.println(set.remove(null));//true
		System.out.println(set.remove(null));//false
	}
}
