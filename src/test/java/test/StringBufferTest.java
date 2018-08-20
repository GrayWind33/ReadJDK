package test;

public class StringBufferTest {
	public static void main(String args[]){
		StringBuffer buff = new StringBuffer();
		buff.append(123);
		//buff.append(null);编译不通过
		String s1 = null;
		System.out.println(buff.append(s1));//123null
		Object obj = null;
		System.out.println(buff.append(obj));//123nullnull
		System.out.println(buff.delete(3, 5));//123llnull
		System.out.println(buff.replace(2, 5, "as"));//12asnull
		System.out.println(buff.replace(2, 3, "678"));//12678snull
	}
}
