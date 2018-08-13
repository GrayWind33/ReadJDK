package java.util;

public class HashMapTest {
	public static void main(String args[]){
		Map<String, String> map = new HashMap<>();
		System.out.println(map.put(null, "123"));//null
		System.out.println(map.put("456", null));//null
		System.out.println(map.get("123"));//null
		System.out.println(map.get(null));//123
		System.out.println(map.get("456"));//null
		System.out.println(map.put(null, "345"));//123
		System.out.println(map.get(null));//345
		map.keySet().remove("456");
		System.out.println(map.get("456"));//null
	}
}
