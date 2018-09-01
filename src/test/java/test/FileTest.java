package test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
		
		//测试filechannel位置改变对stream的影响
		out = new FileOutputStream("D:/test/file.txt");
		out.write("1234567890".getBytes());//1234567890
		out.close();
		
		FileInputStream in = new FileInputStream("D:/test/file.txt");
		
		System.out.print(String.valueOf((byte)in.read() & 0xf));//1
		System.out.print(String.valueOf((byte)in.read() & 0xf));//2
		System.out.print(String.valueOf((byte)in.read() & 0xf));//3
		
		in.getChannel().position(5);
		System.out.print(String.valueOf((byte)in.read() & 0xf));//6
		in.getChannel().position(0);
		System.out.print(String.valueOf((byte)in.read() & 0xf));//1
		
		in.skip(-1);//向前跳一位
		System.out.print(String.valueOf((byte)in.read() & 0xf));//1
		
		BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream("D:/test/file.txt"));
		bout.write("1234567890".getBytes());//1234567890
		bout.close();
		bout.write("1234567890".getBytes());//这里写入的数据在缓冲区的数组中所以不报错
		bout.flush();//这里会报错
	}
}
