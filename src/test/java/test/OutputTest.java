package test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class OutputTest {
	public static void main(String args[]) throws IOException{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write("1234567890".getBytes());
		byte buf[] = out.toByteArray();
		ByteArrayInputStream in = new ByteArrayInputStream(buf);
		byte res[] = new byte[10];
		in.read(res);
		System.out.println(new String(res));//1234567890
		buf[0] = '4';
		in.reset();
		in.read(res);
		System.out.println(new String(res));//4234567890
	}
}
