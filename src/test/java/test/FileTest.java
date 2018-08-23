package test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileTest {
	public static void main(String args[]) throws IOException{
		FileOutputStream out = new FileOutputStream("D:/test/file.txt");
		out.write("1234567890".getBytes());//1234567890
		out.close();
		out = new FileOutputStream("D:/test/file.txt");
		out.write("765".getBytes());//765
		out.close();
		out = new FileOutputStream("D:/test/file.txt", true);
		out.write("asdfgh".getBytes());//765asdfgh
		out.close();
		new File("D:/test/file.txt");//765asdfgh
		out = new FileOutputStream("D:/test/file.txt");
		out.close();//内容为空
	}
}
